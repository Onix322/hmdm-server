// Localization completed
angular
  .module("headwind-kiosk")
  .factory(
    "authService",
    function ($cookies, $window, $rootScope, serverAuthService) {
      var user;
      if ($cookies.get("user")) {
        user = JSON.parse($cookies.get("user"));
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
        loginOIDC: function (successCallback) {
          serverAuthService.loginOidc(function (response) {
            if (response.status === "OK") {
              var redirectUrl = response.data.redirectUrl;
              successCallback(redirectUrl);
            } else {
              console.error("OIDC data cannot be fetch from server");
            }
          });
        },

        handleOidcCallback: function (
          code,
          state,
          successCallback,
          errorCallback,
        ) {
          if (typeof successCallback !== "function")
            successCallback = angular.noop;
          if (typeof errorCallback !== "function") errorCallback = angular.noop;

          serverAuthService.callbackOidc(
            { code: code, state: state },
            {},
            function (response) {
              if (response.status === "OK") {
                var userView = response.data;
                $cookies.put("user", JSON.stringify(userView));
                $rootScope.$broadcast("aero_USER_AUTHENTICATED");
                if (typeof successCallback === "function")
                  successCallback(userView);
              } else {
                if (typeof errorCallback === "function")
                  errorCallback(response);
              }
            },
            function (error) {
              if (typeof errorCallback === "function") errorCallback(error);
            },
          );
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
    },
  )
  .factory("serverAuthService", function ($resource) {
    return $resource(
      "rest/public/auth/",
      {},
      {
        login: { url: "rest/public/auth/login", method: "POST" },
        loginOidc: { url: "rest/public/auth/login-oidc", method: "GET" },
        callbackOidc: { url: "rest/public/auth/callback-oidc", method: "POST" },
        logout: { url: "rest/public/auth/logout", method: "POST" },
        options: { url: "rest/public/auth/options", method: "GET" },
      },
    );
  });
