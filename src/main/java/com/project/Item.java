package com.project;

import java.io.Serializable;

/**
 * Classe que representa un article/producte dins d'un carret.
 * 
 * SERIALIZABLE: Permet que l'objecte es pugui convertir a una seqüència de bytes
 * per a persistència, transmissió per xarxa o emmagatzematge en sessió.
 */
public class Item implements Serializable {

    // Identificador únic de l'article (clau primària a la base de dades)
    private long itemId;
    
    // Nom de l'article
    private String name;
    
    /**
     * RELACIÓ MANYTOONE: Referència al carret que conté aquest article.
     * Aquesta és la part "Many" de la relació (molts Items pertanyen a un Cart).
     * És el costat PROPIETARI de la relació bidireccional amb Cart: a la base de dades
     * es tradueix en una columna FK (Foreign Key) a la taula Item que apunta a la taula Cart.
     * El costat invers és Cart.items, que al mapeig porta inverse="true".
     */
    private Cart cart;

    /**
     * CONSTRUCTOR BUIT: Obligatori per Hibernate/JPA.
     * Hibernate necessita crear instàncies buides mitjançant reflexió
     * abans d'omplir els camps amb les dades de la base de dades.
     */
    public Item() {}

    public Item(String name) {
        this.name = name;
    }

    public long getItemId() { return itemId; }
    public void setItemId(long itemId) { this.itemId = itemId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Cart getCart() { return cart; }
    public void setCart(Cart cart) { this.cart = cart; }

    /**
     * STRING.FORMAT: Crea una cadena formatada substituint %d per enters
     * i %s per strings. Més llegible que concatenar amb +.
     */
    @Override
    public String toString() {
        return String.format("Item [ID=%d, Name=%s]", itemId, name);
    }

    /**
     * EQUALS PER ENTITATS: Dues entitats són iguals si tenen el mateix ID.
     * Una entitat encara no persistida (ID = 0) només és igual a ella mateixa.
     * S'utilitza instanceof (i no getClass()) perquè Hibernate pot crear proxies,
     * que són subclasses de l'entitat.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Item other)) return false;
        return itemId != 0 && itemId == other.getItemId();
    }
    
    /**
     * HASHCODE ESTABLE: Retorna sempre el mateix valor per a totes les instàncies.
     * Si depengués de l'ID, canviaria quan la base de dades l'assigna, i una entitat
     * desada en un HashSet abans de persistir-la ja no s'hi trobaria.
     * REGLA FONAMENTAL: Si dos objectes són equals(), han de tenir el mateix hashCode().
     */
    @Override
    public int hashCode() {
        return Item.class.hashCode();
    }    
}
