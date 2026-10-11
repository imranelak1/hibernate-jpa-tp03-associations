package com.example.persistence;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.Map;
import java.util.function.Function;

public final class JpaContext implements AutoCloseable {
    private final EntityManagerFactory factory;
    public JpaContext() { this(Map.of()); }
    public JpaContext(Map<String, ?> properties) { factory = Persistence.createEntityManagerFactory("gestion-reservations-pu", properties); }
    public <T> T read(Function<EntityManager, T> action) {
        EntityManager em = factory.createEntityManager();
        try { return action.apply(em); } finally { em.close(); }
    }
    public <T> T transaction(Function<EntityManager, T> action) {
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin(); T result = action.apply(em); em.getTransaction().commit(); return result;
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw ex;
        } finally { em.close(); }
    }
    @Override public void close() { if (factory.isOpen()) factory.close(); }
}
