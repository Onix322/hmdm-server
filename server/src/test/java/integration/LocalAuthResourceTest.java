package integration;

import static org.junit.Assert.assertEquals;

import com.hmdm.util.CryptoUtil;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpResponse;
import org.junit.Test;
import util.RequestUtil;

public class LocalAuthResourceTest {

  @Test
  public void testLogin() throws IOException, InterruptedException {

    String username = "admin";
    String password = CryptoUtil.getMD5String("admin");
    URI uri = URI.create("http://localhost/rest/public/auth/");

    String jsonBody = "{\"login\":\"" + username + "\",\"password\":\"" + password + "\"}";

    System.out.println(uri.resolve("login"));
    HttpResponse<String> responseLogin = RequestUtil.requestPost(uri.resolve("login"), jsonBody);

    assertEquals(200, responseLogin.statusCode());

    HttpResponse<String> responseLogout = RequestUtil.requestPost(uri.resolve("logout"), "");

    assertEquals(204, responseLogout.statusCode());
  }
}
