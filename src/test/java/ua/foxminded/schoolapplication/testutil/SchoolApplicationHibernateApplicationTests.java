package ua.foxminded.schoolapplication.testutil;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@DataJpaTest
class SchoolApplicationHibernateApplicationTests {

	@Test
	void contextLoads() {
	}

}
