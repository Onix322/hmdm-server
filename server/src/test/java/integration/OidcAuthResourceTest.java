package integration;

import com.hmdm.auth.AuthStrategy;
import com.hmdm.auth.oidc.OidcAuthCredentials;
import com.hmdm.util.AuthHelper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockserver.client.MockServerClient;

public class OidcAuthResourceTest {

  public OidcAuthResourceTest() {}

  @Mock private AuthHelper authHelper;
  @Mock private HttpServletRequest request;
  @Mock private AuthStrategy<OidcAuthCredentials> strategy;

  private static MockServerClient client;
  private static String protocol = "http";
  private static String hostname = "localhost";
  private static Integer port = 1080;
  private static String providerUrl = protocol + "://" + hostname + ":" + port;
  private static String clientId = "my-app";
  private static String audience = "my-api";

  @BeforeClass
  public static void startServer() {}

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void testLoginOidc() throws Exception {}

  @Test
  public void testCallbackOidc() throws Exception {}

  @AfterClass
  public static void stopServer() {
    if (client != null) {
      client.close();
    }
  }
}
