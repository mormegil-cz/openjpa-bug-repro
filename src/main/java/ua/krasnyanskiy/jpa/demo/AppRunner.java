package ua.krasnyanskiy.jpa.demo;

import ua.krasnyanskiy.jpa.demo.dto.Order;
import ua.krasnyanskiy.jpa.demo.dto.User;

import javax.persistence.EntityManager;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static javax.persistence.Persistence.createEntityManagerFactory;

public class AppRunner {
    public static void main(String[] args) throws Exception {
        // both mssql and h2 driver exhibit the same behavior
        final String DRIVER_H2 = "h2";
        final String DRIVER_MSSQL = "mssql";
        final EntityManager entityManager = createEntityManagerFactory(DRIVER_H2).createEntityManager();

        initDatabase(entityManager);

        final List<User> users = entityManager.createQuery("select u from User u", User.class).getResultList();

        boolean isBroken = false;
        for (final User firstUser : users) {
            final List<Integer> results = new ArrayList<>(3);

            final List<User> duplicateUsers = entityManager
                    .createQuery("select u from User u where u.email=:firstEmail and u<>:firstUser and exists (" +
                            "select o1 from Order o1 where o1.user=u and exists(" +
                            "select o2 from Order o2 where o2.user=:firstUser and o1.productCode=o2.productCode" +
                            ")" +
                            ")", User.class)
                    .setParameter("firstUser", firstUser)
                    .setParameter("firstEmail", firstUser.getEmail())
                    .getResultList();
            results.add(duplicateUsers.size());
            System.out.println(String.format("BROKEN QUERY: For user %s: %d duplicate(s) found", firstUser.getName(), duplicateUsers.size()));

            final List<User> alternateQuery = entityManager
                    .createQuery("select u from User u where u.email=:firstEmail and u<>:firstUser and exists (" +
                            "select o1 from Order o1, Order o2 where o1.user=u and o2.user=:firstUser and o1.productCode=o2.productCode" +
                            ")", User.class)
                    .setParameter("firstUser", firstUser)
                    .setParameter("firstEmail", firstUser.getEmail())
                    .getResultList();
            results.add(alternateQuery.size());
            System.out.println(String.format("ALTERNATE QUERY: For user %s: %d duplicate(s) found", firstUser.getName(), alternateQuery.size()));

            final List<Order> orderQuery = entityManager
                    .createQuery("select o from Order o join o.user u where u.email=:firstEmail and u<>:firstUser and o.productCode in (" +
                            "select o1.productCode from Order o1 where o1.user=:firstUser" +
                            ")", Order.class)
                    .setParameter("firstUser", firstUser)
                    .setParameter("firstEmail", firstUser.getEmail())
                    .getResultList();
            results.add(orderQuery.size());
            System.out.println(String.format("FIXED (INVERSE) QUERY: For user %s: %d duplicate(s) found", firstUser.getName(), orderQuery.size()));

            if (!Collections.min(results).equals(Collections.max(results))) {
                isBroken = true;
                System.out.println("Broken: Results do not match!");
            }
        }

        if (isBroken) {
            System.out.println("**** IT IS BROKEN! ****");
        } else {
            System.out.println("All fine!");
        }
    }

    private static void initDatabase(EntityManager entityManager) throws Exception {
        for (final String fileName : Arrays.asList("/sql/ddl.sql", "/sql/dml.sql")) {
            final List<String> commands = Files.readAllLines(Path.of(AppRunner.class.getResource(fileName).toURI()));
            entityManager.getTransaction().begin();
            for(final String command : commands) {
                entityManager.createNativeQuery(command).executeUpdate();
            }
            entityManager.getTransaction().commit();
        }
    }
}
