/* global angular */
/* global angular */
angular
  .module("headwind-kiosk")
  .controller(
    "OidcCallbackController",
    function ($scope, $location, $window, authService, $timeout) {
      $scope.loading = true;
      $scope.errorMessage = null;

      // Extragere cod din URL
      var searchParams = new URLSearchParams($window.location.search);
      var code = searchParams.get("code") || $location.search().code;
      var state = searchParams.get("state") || $location.search().state;

      if (code) {
        authService.handleOidcCallback(
          code,
          state,
          function () {
            $timeout(() => {
              var cleanUrl =
                $window.location.origin + $window.location.pathname + "#/main";
              $window.location.replace(cleanUrl);
            }, 2000);
          },
          function (error) {
            $scope.loading = false;
            $scope.errorMessage =
              error && error.message
                ? error.message
                : "OIDC authentification has failed or session has expired.";
          },
        );
      } else {
        $scope.loading = false;
        $scope.errorMessage = "Authorization code is missing in the url.";
      }
    },
  );
