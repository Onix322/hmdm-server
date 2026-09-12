// Localization completed
angular
  .module("headwind-kiosk")
  .factory("authService", function ($cookies, serverAuthService) {
    var user;
    var oidcToken;
    if ($cookies.get("user")) {
      user = JSON.parse($cookies.get("user"));
    }

    if ($cookies.get("oidc_token")) {
      oidcToken = $cookies.get("oidc_token");
    }

    return {
      loginLocal: function (login, password, successCallback) {
        serverAuthService.login(
          { login: login, password: password },
          function (response) {
            if (response.status === "OK") {
              user = response.data;
              var userStr = JSON.stringify(user);
              $cookies.put("user", userStr);
            }

            successCallback(response);
          },
        );
      },

      options: function (successCallback) {
        serverAuthService.options(successCallback);
      },

      // OIDC logic
      // oidcData = { authorize_url, client_id, redirect_uri, "scope", "state". "response_type" }
      loginOIDC: function () {
        serverAuthService.oicdDetails(function (response) {
          if (response.status === "OK") {
            var oidcData = response.data;
            console.log(oidcData);
            var url =
              oidcData.authorizeUrl +
              "?client_id=" +
              encodeURIComponent(oidcData.clientId) +
              "&response_type=" +
              encodeURIComponent(oidcData.responseType) +
              "&redirect_uri=" +
              encodeURIComponent(oidcData.redirectUri) +
              "&scope=" +
              encodeURIComponent(oidcData.scope) +
              "&state=" +
              encodeURIComponent(oidcData.state);

            window.location.href = url;
          } else {
            console.error("OIDC data cannot be fetch from server");
          }
        });
      },

      handleOidcCallback: function (code, state) {
        // send codeParam / state to backend
        // backend will exchange those for id_token and user infos
        // retrive UserInfo (UserView)
        // set a cookie with user like the normal one
        // redirect to main on success
      },

      hasPermission: function (permission) {
        if (user) {
          if (user.userRole) {
            if (user.userRole.superAdmin) {
              return true;
            } else {
              if (user.userRole.permissions) {
                return (
                  user.userRole.permissions.find(function (p) {
                    return p.name === permission;
                  }) !== undefined
                );
              }
            }
          }
        }

        return false;
      },

      logout: function () {
        serverAuthService.logout();

        user = undefined;
        $cookies.remove("user");
        $cookies.remove("deviceSearch");
      },

      update: function (newUser) {
        user = newUser;
        $cookies.put("user", JSON.stringify(newUser));
      },

      isLoggedIn: function () {
        return user !== undefined;
      },

      isSuperAdmin: function () {
        return user && user.userRole.superAdmin;
      },

      isSingleCustomer: function () {
        return user && user.singleCustomer;
      },

      getUserName: function () {
        return user ? user.name : undefined;
      },
      getUserLogin: function () {
        return user ? user.login : undefined;
      },
      getId: function () {
        return user ? user.id : undefined;
      },
      getUser: function () {
        var result = {};
        for (var p in user) {
          if (user.hasOwnProperty(p)) {
            result[p] = user[p];
          }
        }

        return result;
      },
    };
  })
  .factory("serverAuthService", function ($resource) {
    return $resource(
      "rest/public/auth/",
      {},
      {
        login: { url: "rest/public/auth/login", method: "POST" },
        logout: { url: "rest/public/auth/logout", method: "POST" },
        options: { url: "rest/public/auth/options", method: "GET" },
        oicdDetails: { url: "rest/public/auth/oidc-details", method: "GET" },
      },
    );
  });
