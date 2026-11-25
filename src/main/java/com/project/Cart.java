package com.project;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Classe que representa un carret de compra.
 * 
 * SERIALIZABLE: Implementa Serializable per permetre que els objectes Cart
 * es puguin convertir a bytes (per guardar-los a disc, enviar per xarxa, etc.)
 */
public class Cart implements Serializable {
    
    // Identificador únic del carret (clau primària a la base de dades)
    private long cartId;
    
    // Tipus de carret (exemple: "premium", "standard", etc.)
    private String type;  
    
    /**
     * HASHSET: Col·lecció que emmagatzema els Items d'aquest carret.
     * - HashSet no permet duplicats (utilitza equals/hashCode per comparar)
     * - No garanteix cap ordre dels elements
     * - Aquesta és la part "One" de la relació OneToMany amb Item
     */
    private Set<Item> items = new HashSet<>();

    // Constructor buit requerit per Hibernate/JPA
    public Cart() {}

    public Cart(String type) {
        this.type = type;
    }

    public long getCartId() { return cartId; }
    public void setCartId(long cartId) { this.cartId = cartId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    /**
     * Retorna la col·lecció directa perquè Hibernate pugui gestionar-la.
     * IMPORTANT: No retornem Collections.unmodifiableSet() perquè trencaria
     * el "Dirty Checking" de Hibernate (detecció automàtica de canvis).
     */
    public Set<Item> getItems() {
        return items;
    }
  
    /**
     * No se substitueix mai la col·lecció que gestiona Hibernate (this.items = ...):
     * es modifica amb els mètodes d'ajuda, que mantenen els dos costats sincronitzats.
     */
    public void setItems(Set<Item> newItems) {
        Set<Item> desired = (newItems == null) ? new HashSet<>() : new HashSet<>(newItems);
        for (Item current : new HashSet<>(this.items)) {
            if (!desired.contains(current)) {
                removeItem(current);
            }
        }
        for (Item item : desired) {
            addItem(item);
        }
    }
  
    /**
     * GESTIÓ BIDIRECCIONAL: Quan afegim un Item al carret, també hem d'actualitzar
     * la referència inversa (item.setCart(this)) per mantenir la coherència
     * entre les dues bandes de la relació.
     */
    public void addItem(Item item) {
        if (items.add(item)) {
            item.setCart(this);
        }
    }

    public void removeItem(Item item) {
        if (items.remove(item)) {
            item.setCart(null);
        }
    }
  
    /**
     * STREAMS I COLLECTORS: Utilitza l'API de Streams de Java 8+ per transformar
     * la col·lecció d'items en una representació en text.
     * - stream(): converteix la col·lecció en un flux de dades
     * - map(): transforma cada Item al seu nom (String)
     * - Collectors.joining(): uneix tots els noms amb ", " i afegeix "[" i "]"
     *
     * En general un toString() no hauria de recórrer una relació: pot llançar consultes
     * inesperades o recursió infinita. Aquí és segur perquè la col·lecció es carrega
     * sempre amb el Cart (lazy="false" al mapeig) i només es llegeix el nom de cada Item.
     */
    @Override
    public String toString() {
        String llistaItems = "[]";
        
        if (items != null && !items.isEmpty()) {
            llistaItems = items.stream()
                .map(Item::getName)
                .collect(Collectors.joining(", ", "[", "]"));
        }

        return String.format("Cart [ID=%d, Type=%s, Items: %s]", cartId, type, llistaItems);
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
        if (!(o instanceof Cart other)) return false;
        return cartId != 0 && cartId == other.getCartId();
    }
    
    /**
     * HASHCODE ESTABLE: Retorna sempre el mateix valor per a totes les instàncies.
     * Si depengués de l'ID, canviaria quan la base de dades l'assigna, i una entitat
     * desada en un HashSet abans de persistir-la ja no s'hi trobaria.
     * REGLA FONAMENTAL: Si dos objectes són equals(), han de tenir el mateix hashCode().
     */
    @Override
    public int hashCode() {
        return Cart.class.hashCode();
    }    
}
