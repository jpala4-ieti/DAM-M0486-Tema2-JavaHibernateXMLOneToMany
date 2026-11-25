package com.project;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.Hibernate;
import org.hibernate.Session; 
import org.hibernate.Transaction;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.NativeQuery;

/**
 * Classe Manager: Gestiona totes les operacions amb la base de dades.
 * Actua com a capa d'accés a dades (DAO - Data Access Object).
 * Centralitza la lògica de Hibernate per simplificar el codi a Main.
 */
public class Manager {

    /**
     * SESSIONFACTORY: Objecte principal d'Hibernate. És THREAD-SAFE i s'ha
     * de crear només UN COP durant tota l'aplicació (costós de crear).
     * Funciona com una fàbrica que produeix Sessions sota demanda.
     */
    private static SessionFactory factory;

    /**
     * Carrega la configuració d'Hibernate des de hibernate.cfg.xml
     * i crea la SessionFactory. S'ha de cridar al principi de l'aplicació.
     */
    public static void createSessionFactory() {
        try {
            factory = new Configuration().configure().buildSessionFactory();
        } catch (Throwable ex) { 
            System.err.println("Failed to create sessionFactory object." + ex);
            throw new ExceptionInInitializerError(ex); 
        }
    }

    public static void close() {
        if (factory != null) factory.close();
    }
  
    // ============================================================
    // GESTIÓ DE TRANSACCIONS - PATRÓ DRY (Don't Repeat Yourself)
    // ============================================================

    /**
     * CONSUMER<SESSION>: Interfície funcional que accepta un paràmetre
     * (Session) i no retorna res (void). Perfecte per operacions com
     * insert, update, delete.
     * 
     * PATRÓ TRY-WITH-RESOURCES: El "try (Session session = ...)" garanteix
     * que la sessió es tancarà automàticament al final, fins i tot si
     * hi ha excepcions. Equivalent a fer session.close() en un finally.
     * 
     * TRANSACCIÓ: Conjunt d'operacions que s'executen com una unitat atòmica.
     * - Si tot va bé → commit() (guarda els canvis)
     * - Si hi ha error → rollback() (desfà tots els canvis)
     */
    private static void executeInTransaction(Consumer<Session> action) {
        try (Session session = factory.openSession()) {
            Transaction tx = session.beginTransaction();
            try {
                action.accept(session);
                tx.commit();
            } catch (Exception e) {
                // El rollback es fa abans que el try-with-resources tanqui la sessió
                tx.rollback();
                throw new RuntimeException("Error en transacció Hibernate", e);
            }
        }
    }

    /**
     * FUNCTION<SESSION, T>: Interfície funcional que accepta un paràmetre
     * (Session) i RETORNA un valor de tipus T. Perfecte per operacions
     * de lectura (SELECT) o quan necessitem l'objecte creat.
     * 
     * GENÈRICS <T>: Permet que el mètode retorni qualsevol tipus d'objecte
     * (Cart, Item, List<Cart>, etc.) sense duplicar codi.
     * 
     * NOTA: Hibernate ja porta aquests dos mètodes fets: factory.inTransaction(...)
     * i factory.fromTransaction(...). Aquí s'escriuen a mà per veure què fan.
     */
    private static <T> T executeInTransactionWithResult(Function<Session, T> action) {
        try (Session session = factory.openSession()) {
            Transaction tx = session.beginTransaction();
            try {
                T result = action.apply(session);
                tx.commit();
                return result;
            } catch (Exception e) {
                tx.rollback();
                throw new RuntimeException("Error en transacció Hibernate", e);
            }
        }
    }

    // ============================================================
    // OPERACIONS CRUD (Create, Read, Update, Delete)
    // ============================================================

    /**
     * PERSIST: Diu a Hibernate que gestioni aquest objecte NOU.
     * L'objecte passa a estat "managed" i es guardarà a la BD quan
     * es faci commit(). Hibernate assignarà l'ID automàticament.
     */
    public static Cart addCart(String type) {
        return executeInTransactionWithResult(session -> {
            Cart cart = new Cart(type);
            session.persist(cart);
            return cart;
        });
    }

    public static Item addItem(String name) {
        return executeInTransactionWithResult(session -> {
            Item item = new Item(name);
            session.persist(item);
            return item;
        });
    }

    /**
     * FIND: Recupera una entitat per la seva clau primària (ID).
     * Retorna null si no existeix. (get() està obsolet des de Hibernate 7.)
     * 
     * DIRTY CHECKING: N'hi ha prou de modificar l'objecte. En fer commit, Hibernate
     * el compara amb com era quan el va carregar i genera l'UPDATE tot sol.
     * No cal cridar cap update() ni merge().
     */
    public static void updateItem(long itemId, String name) {
        executeInTransaction(session -> {
            Item item = session.find(Item.class, itemId);
            if (item != null) {
                item.setName(name);
            }
        });
    }

    /**
     * ACTUALITZACIÓ DE RELACIONS BIDIRECCIONALS:
     * 1. Primer netegem la relació existent (removeItem desvincula cada Item)
     * 2. Després afegim els nous Items (addItem vincula cada Item al Cart)
     * 
     * LIST.COPYOF: Creem una còpia immutable de la col·lecció per evitar
     * ConcurrentModificationException (no pots modificar una col·lecció
     * mentre la recorres amb un forEach).
     */
    public static void updateCart(long cartId, String type, Set<Item> newItems) {
        executeInTransaction(session -> {
            Cart cart = session.find(Cart.class, cartId);
            if (cart == null) return;
            
            cart.setType(type);
            
            // Si newItems és null, no toquem les relacions existents
            if (newItems != null) {

                // 1. Netejar items existents
                if (cart.getItems() != null && !cart.getItems().isEmpty()) {
                    List<Item> itemsToRemove = List.copyOf(cart.getItems());
                    itemsToRemove.forEach(cart::removeItem);
                }

                // 2. Afegir nous items (recuperant-los com a "managed")
                for (Item item : newItems) {
                    Item managedItem = session.find(Item.class, item.getItemId());
                    if (managedItem != null) {
                        cart.addItem(managedItem);
                    }
                }
            }

            // No cal merge(): el Cart l'ha carregat aquesta sessió i el dirty checking
            // detecta tots els canvis en fer commit.
        });
    }
    
    /**
     * HIBERNATE.INITIALIZE: Força la càrrega d'una col·lecció LAZY.
     * 
     * LAZY LOADING: Per defecte, les col·leccions (@OneToMany) NO es carreguen
     * fins que s'accedeixen. Si la sessió ja està tancada quan hi accedim,
     * obtenim LazyInitializationException. Amb initialize() carreguem
     * les dades ABANS de tancar la sessió.
     */
    public static Cart getCartWithItems(long cartId) {
        return executeInTransactionWithResult(session -> {
            Cart cart = session.find(Cart.class, cartId);
            if (cart != null) {
                Hibernate.initialize(cart.getItems());
            }
            return cart;
        });
    }

    /**
     * MÈTODE GENÈRIC: Class<T> permet passar qualsevol tipus d'entitat
     * com a paràmetre (Cart.class, Item.class, etc.).
     * Evita duplicar codi per cada entitat.
     */
    public static <T> T getById(Class<T> clazz, long id) {
        return executeInTransactionWithResult(session -> session.find(clazz, id));
    }

    /**
     * REMOVE: Marca l'entitat per ser eliminada de la base de dades.
     * L'eliminació real passa quan es fa commit().
     */
    public static <T> void delete(Class<T> clazz, Serializable id) {
        executeInTransaction(session -> {
            T obj = session.find(clazz, id);
            if (obj != null) {
                session.remove(obj);
            }
        });
    }

    /**
     * HQL (Hibernate Query Language): Llenguatge de consultes similar a SQL
     * però treballa amb OBJECTES (entitats) en lloc de taules.
     * 
     * "FROM Cart" retorna objectes Cart, no files de taula.
     * clazz.getName() retorna el nom complet de la classe (com.project.Cart).
     */
    public static <T> List<T> listCollection(Class<T> clazz, String whereClause) {
        return listCollection(clazz, whereClause, Map.of());
    }

    /**
     * PARÀMETRES AMB NOM: Els valors de la condició no es concatenen mai dins de
     * la consulta (risc d'injecció). S'escriuen com a :nom i es passen a part:
     * 
     *   listCollection(Item.class, "name LIKE :name", Map.of("name", "%Test%"))
     * 
     * El whereClause només ha de contenir text fix escrit pel programador.
     */
    public static <T> List<T> listCollection(Class<T> clazz, String whereClause, Map<String, Object> params) {
        return executeInTransactionWithResult(session -> {
            String hql = "FROM " + clazz.getName();
            if (whereClause != null && !whereClause.trim().isEmpty()) {
                hql += " WHERE " + whereClause;
            }
            var query = session.createQuery(hql, clazz);
            params.forEach((name, value) -> query.setParameter(name, value));
            return query.getResultList();
        });
    }

    /**
     * SOBRECÀRREGA DE MÈTODES: Dos mètodes amb el mateix nom però
     * diferent signatura (paràmetres). Permet cridar listCollection
     * sense whereClause quan volem tots els registres.
     */
    public static <T> List<T> listCollection(Class<T> clazz) {
        return listCollection(clazz, "");
    }

    /**
     * STRINGBUILDER: Més eficient que concatenar Strings amb +
     * quan es fan moltes concatenacions (dins un bucle).
     * Cada + amb String crea un objecte nou; StringBuilder modifica el mateix.
     */
    public static <T> String collectionToString(Collection<T> collection) {
        if (collection == null || collection.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder();
        for (T obj : collection) {
            sb.append(obj.toString()).append("\n");
        }
        return sb.toString();
    }

    // ============================================================
    // CONSULTES SQL NATIVES
    // ============================================================

    /**
     * NATIVEQUERY: Permet executar SQL pur (no HQL).
     * Útil per operacions específiques de la BD o optimitzacions.
     * createNativeMutationQuery() s'usa per INSERT, UPDATE, DELETE;
     * executeUpdate() retorna el nombre de files afectades.
     */
    public static void queryUpdate(String queryString) {
        executeInTransaction(session -> {
            session.createNativeMutationQuery(queryString).executeUpdate();
        });
    }

    /**
     * Object[]: Cada fila del resultat és un array d'objectes.
     * Cada posició de l'array correspon a una columna del SELECT.
     * Exemple: SELECT id, name FROM items → [0]=id, [1]=name
     */
    public static List<Object[]> queryTable(String queryString) {
        return executeInTransactionWithResult(session -> {
            NativeQuery<Object[]> query = session.createNativeQuery(queryString, Object[].class);
            return query.getResultList();
        });
    }
}
