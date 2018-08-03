package kz.bsbnb.testing;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.sql.DataSource;
import java.io.InputStream;

@ActiveProfiles("dev")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/applicationContextProp.xml","/applicationContextTest.xml"})
public class FunctionalTest extends BaseUnitTest {
    @Autowired
    protected DataSource dataSource;

    public InputStream getInputStream(String file){
        return Thread.currentThread().
                getContextClassLoader().getResourceAsStream("kz/bsbnb/test/" + file);

    }
}
