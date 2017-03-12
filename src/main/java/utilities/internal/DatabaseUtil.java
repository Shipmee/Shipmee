package utilities.internal;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import javax.persistence.metamodel.EmbeddableType;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceProviderResolver;
import javax.persistence.spi.PersistenceProviderResolverHolder;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.ImprovedNamingStrategy;
import org.hibernate.dialect.Dialect;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.jdbc.Work;
import org.hibernate.jpa.HibernatePersistenceProvider;

import utilities.DatabaseConfig;
import domain.DomainEntity;

public class DatabaseUtil {

	// Constructor ------------------------------------------------------------

	public DatabaseUtil() throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		resolver = PersistenceProviderResolverHolder.getPersistenceProviderResolver();
		providers = resolver.getPersistenceProviders();
		persistenceProvider = new HibernatePersistenceProvider();
		entityManagerFactory = persistenceProvider.createEntityManagerFactory(DatabaseConfig.PersistenceUnit, null);
		entityManager = entityManagerFactory.createEntityManager();

		properties = entityManagerFactory.getProperties();
		// printProperties(properties);

		databaseUrl = findProperty("javax.persistence.jdbc.url");
		databaseName = StringUtils.substringAfterLast(databaseUrl, "/");
		databaseDialectName = findProperty("hibernate.dialect");
		databaseDialect = (Dialect) ReflectHelper.classForName(databaseDialectName).newInstance();

		configuration = buildConfiguration();

		configuration.setNamingStrategy(ImprovedNamingStrategy.class.newInstance());
		
		entityTransaction = entityManager.getTransaction();
	}


	// Properties -------------------------------------------------------------

	private final PersistenceProviderResolver	resolver;
	private final PersistenceProvider			persistenceProvider;
	private final EntityManagerFactory			entityManagerFactory;
	private final EntityManager					entityManager;
	private final Map<String, Object>			properties;
	private final String						databaseUrl;
	private final String						databaseName;
	private final String						databaseDialectName;
	private final Dialect						databaseDialect;
	private final Configuration					configuration;
	private final EntityTransaction				entityTransaction;
	private final List<PersistenceProvider>		providers;


	public PersistenceProviderResolver getResolver() {
		return resolver;
	}

	public PersistenceProvider getPersistenceProvider() {
		return persistenceProvider;
	}

	public EntityManagerFactory getEntityManagerFactory() {
		return entityManagerFactory;
	}

	public EntityManager getEntityManager() {
		return entityManager;
	}

	public Map<String, Object> getProperties() {
		return properties;
	}

	public String getDatabaseUrl() {
		return databaseUrl;
	}

	public String getDatabaseName() {
		return databaseName;
	}

	public String getDatabaseDialectName() {
		return databaseDialectName;
	}

	public Dialect getDatabaseDialect() {
		return databaseDialect;
	}

	public Configuration getConfiguration() {
		return configuration;
	}

	public EntityTransaction getEntityTransaction() {
		return entityTransaction;
	}

	public List<PersistenceProvider> getProviders() {
		return providers;
	}

	// Business methods -------------------------------------------------------

	//	public EntityManagerFactory createEntityManagerFactory(String persistenceUnitName, Map<?, ?> properties) {
	//		EntityManagerFactory result;
	//		PersistenceProvider defaultProvider;
	//		List<PersistenceProvider> providers;
	//		PersistenceProvider provider;
	//		Iterator<PersistenceProvider> iterator;
	//		boolean done;
	//
	//		result = null;
	//		defaultProvider = null;
	//		providers = getProviders();
	//
	//		done = false;
	//		iterator = providers.iterator();
	//		while (!done && iterator.hasNext()) {
	//			provider = iterator.next();
	//			if (provider instanceof HibernatePersistenceProvider) {
	//				result = provider.createEntityManagerFactory(persistenceUnitName, properties);
	//				done = (result != null);
	//			}
	//		}
	//		if (result == null) {
	//			defaultProvider = new HibernatePersistenceProvider();
	//			result = defaultProvider.createEntityManagerFactory(persistenceUnitName, properties);
	//		}
	//		if (result == null) {
	//			throw new PersistenceException("No persistence provider found");
	//		}
	//		return result;
	//	}

	public void recreateDatabase() throws Throwable {
		List<String> databaseScript;
		List<String> schemaScript;
		String[] statements;

		databaseScript = new ArrayList<String>();
		databaseScript.add(String.format("drop database `%s`", databaseName));
		databaseScript.add(String.format("create database `%s`", databaseName));
		executeScript(databaseScript);

		schemaScript = new ArrayList<String>();
		schemaScript.add(String.format("use `%s`", databaseName));
		statements = configuration.generateSchemaCreationScript(databaseDialect);
		schemaScript.addAll(Arrays.asList(statements));
		executeScript(schemaScript);
	}

	public void openTransaction() {
		entityTransaction.begin();
	}

	public void commitTransaction() {
		entityTransaction.commit();
	}

	public void rollbackTransaction() {
		entityTransaction.rollback();
	}

	public void persist(DomainEntity entity) {
		entityManager.persist(entity);
		// entityManager.flush();
	}

	public void close() {
		if (entityTransaction.isActive())
			entityTransaction.rollback();
		if (entityManager.isOpen())
			entityManager.close();
		if (entityManagerFactory.isOpen())
			entityManagerFactory.close();
	}

	public int executeUpdate(String line) {
		int result;
		Query query;

		query = entityManager.createQuery(line);
		result = query.executeUpdate();

		return result;
	}

	public List<?> executeSelect(String line) {
		List<?> result;
		Query query;

		query = entityManager.createQuery(line);
		result = query.getResultList();

		return result;
	}

	// Ancillary methods ------------------------------------------------------

	protected Configuration buildConfiguration() {
		Configuration result;
		Metamodel metamodel;
		Collection<EntityType<?>> entities;
		Collection<EmbeddableType<?>> embeddables;

		result = new Configuration();
		metamodel = entityManagerFactory.getMetamodel();

		entities = metamodel.getEntities();
		for (EntityType<?> entity : entities)
			result.addAnnotatedClass(entity.getJavaType());

		embeddables = metamodel.getEmbeddables();
		for (EmbeddableType<?> embeddable : embeddables)
			result.addAnnotatedClass(embeddable.getJavaType());

		return result;
	}

	protected void executeScript(final List<String> script) {
		Session session;
		session = entityManager.unwrap(Session.class);
		session.doWork(new Work() {

			@Override
			public void execute(Connection connection) throws SQLException {
				Statement statement = null;

				try {
					statement = connection.createStatement();
					for (String line : script) {
						statement.execute(line);
					}
					connection.commit();
				} finally {
					statement.close();
				}
			}
		});
	}

	protected String findProperty(String property) {
		String result;
		Object value;

		value = properties.get(property);
		if (value == null)
			throw new RuntimeException(String.format("Property `%s' not found", property));
		if (!(value instanceof String))
			throw new RuntimeException(String.format("Property `%s' is not a string", property));
		result = (String) value;
		if (StringUtils.isBlank(result))
			throw new RuntimeException(String.format("Property `%s' is blank", property));

		return result;
	}

	protected void printProperties(Map<String, Object> properties) {
		for (Entry<String, Object> entry : properties.entrySet())
			System.out.println(String.format("%s=`%s'", entry.getKey(), entry.getValue()));
	}

}
