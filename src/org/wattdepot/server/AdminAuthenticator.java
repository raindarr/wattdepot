package org.wattdepot.server;

import org.restlet.Context;
import org.restlet.security.ChallengeAuthenticator;
import org.restlet.data.ChallengeScheme;

/**
 * Performs the authentication of HTTP requests that require Admin Access Control Level, currently
 * using HTTP Basic authentication. See
 * http://code.google.com/p/wattdepot/wiki/RestApi#Access_Control_Levels for more information.
 * 
 * @author Robert Brewer
 */
public class AdminAuthenticator extends ChallengeAuthenticator {

  /**
   * Creates the AdminAuthenticator for HTTP Basic authentication.
   * 
   * @param context the Restlet context
   */
  public AdminAuthenticator(Context context) {
    super(context, ChallengeScheme.HTTP_BASIC, "WattDepot");
  }

  /**
   * Checks whether the provided credentials are valid.
   * 
   * @param request The Restlet request.
   * @param identifier The account name.
   * @param secret The password.
   * @return True if the credentials are valid.
   */
  //TODO: This doesn't work with Restlet 2.0, but it isn't being used now anyway 
 /* @Override
  public boolean checkSecret(Request request, String identifier, char[] secret) {
    ServerProperties serverProps = (ServerProperties) getContext().getAttributes().get(
        "ServerProperties");
    String adminUsername = serverProps.get(ServerProperties.ADMIN_EMAIL_KEY);
    String adminPassword = serverProps.get(ServerProperties.ADMIN_PASSWORD_KEY);

    Server server = (Server) getContext().getAttributes().get("WattDepotServer");
    server.getLogger().fine(
        "request username: " + identifier + ", request password: " + new String(secret)
            + ", admin username: " + adminUsername + ", admin password: " + adminPassword);
    // For now, only accept requests from the admin user
    return identifier.equals(adminUsername) && new String(secret).equals(adminPassword);
  }*/
}
