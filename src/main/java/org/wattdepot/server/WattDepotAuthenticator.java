package org.wattdepot.server;

import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.security.ChallengeAuthenticator;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Status;
import org.wattdepot.server.db.DbManager;

/**
 * Performs the authentication of HTTP requests for WattDepot, currently using HTTP Basic
 * authentication. Calls WattDepotVerifier and WattDepotEnroler to perform authentication and add
 * users to roles, respectively.
 * 
 * @author Andrea Connell
 */
public class WattDepotAuthenticator extends ChallengeAuthenticator {

  /**
   * Creates the WattDepotAuthenticator for HTTP Basic authentication.
   * 
   * @param context the Restlet context
   */
  public WattDepotAuthenticator(Context context) {
    super(context, ChallengeScheme.HTTP_BASIC, "WattDepot");
    setVerifier(new WattDepotVerifier((DbManager) getContext().getAttributes().get("DbManager")));
    setEnroler(new WattDepotEnroler((DbManager) getContext().getAttributes().get("DbManager")));
    setOptional(true);
  }

  /**
   * Authenticate if a ChallengeResponse is supplied. Otherwise attempt anonymous access.
   * 
   * @param request The request to be authenticated.
   * @param response The response to be authenticated.
   * @return True if the user has been authenticated.
   */
  @Override
  protected boolean authenticate(Request request, Response response) {
    if (request.getChallengeResponse() == null) {
      return false;
    }
    else {
      return super.authenticate(request, response);
    }
  }

  /**
   * Set an UNAUTHORIZED status (rather than the default FORBIDDEN) to match with the client's
   * expectation.
   * 
   * @param response The response.
   */
  @Override
  public void forbid(Response response) {
    response.setStatus(Status.CLIENT_ERROR_UNAUTHORIZED);
  }
}
