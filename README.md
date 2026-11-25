# Exemple Canvas Hibernate tipus XML One To Many (obsolet)

En aquest projecte hi ha un exemple de Hibernate tipus XML One To Many

## Compilació i execució

Cal Java 21 i 'Maven' per compilar el projecte
```bash
mvn clean
mvn compile
mvn test
```

### Execució amb mvn
La manera més directa. Compila i executa la classe principal definida al `pom.xml` (`com.project.Main`):
```bash
mvn compile exec:java
```

Per executar una altra classe principal, per exemple la que mostra el contingut de la base de dades SQLite:
```bash
mvn compile exec:java "-Dexec.mainClass=com.project.utils.MainSQLite"
```

Per passar paràmetres a l'aplicació:
```bash
mvn compile exec:java "-Dexec.mainClass=com.project.Main" "-Dexec.args=param1 param2 param3"
```

On:
* `exec.mainClass`: és la classe principal que vols executar.
* `exec.args`: són els paràmetres que necessites passar a la teva aplicació, separats per espais.

Afegeix `-q` (`mvn -q compile exec:java`) per veure només la sortida del programa, sense els missatges de Maven.

### Execució amb script
Els scripts fan el mateix que l'apartat anterior i, a més, defineixen les opcions `--add-opens` de Java a la variable `MAVEN_OPTS`.

Per executar el projecte a Windows cal
```bash
.\run.ps1 com.project.Main
```

Per executar el projecte a Linux/macOS cal
```bash
./run.sh com.project.Main
```

### Execució directa amb java
Preparar les dependències en una carpeta (Recomanada per a proves)
```bash
mvn clean package dependency:copy-dependencies
```

A Windows (el separador del classpath és `;`):
```bash
java --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED -cp "target/classes;target/dependency/*" com.project.Main
```

A Linux/macOS (el separador del classpath és `:`):
```bash
java --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED -cp "target/classes:target/dependency/*" com.project.Main
```

### Execució directa amb java (jar package)
`mvn clean package` genera un jar únic amb totes les dependències:
```bash
mvn clean package
java --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED -jar target/hibernate-xml-one-to-many-1.0.0.jar
```

### Execució de tests
```bash
# Executar TOTS els tests
mvn test
# Executar una classe de test, amb el package o només amb el nom
mvn test "-Dtest=com.project.CartItemTest"
mvn test "-Dtest=CartItemTest"
# Executar un sol mètode de test
mvn test "-Dtest=CartItemTest#testCreateCart"
# Executar diversos mètodes de test (separats per +)
mvn test "-Dtest=CartItemTest#testCreateCart+testCreateItems"
# Totes les classes de test que comencin amb "Cart"
mvn test "-Dtest=Cart*"
# Totes les classes de test que continguin "Item"
mvn test "-Dtest=*Item*"
```

## Docker per treballar amb mysql

### Iniciar el contenedor
docker-compose up -d

### Verificar que está funcionando
docker-compose ps

### Ver los logs si hay algún problema
docker-compose logs

### Para detener el contenedor
docker-compose down