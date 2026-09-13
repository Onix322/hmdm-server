/* global angular */
/* global angular */
angular
  .module("headwind-kiosk")
  .controller(
    "OidcCallbackController",
    function ($scope, $state, $location, $window, authService) {
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
          function (userView) {
            var cleanUrl =
              $window.location.origin + $window.location.pathname + "#/main";
            $window.location.replace(cleanUrl);
            $scope.successMessage = "Logged in";
          },
          function (error) {
            $scope.loading = false;
            $scope.errorMessage =
              error && error.message
                ? error.message
                : "Autentificarea OIDC a eșuat sau sesiunea a expirat.";
          },
        );
      } else {
        $scope.loading = false;
        $scope.errorMessage = "Codul de autorizare OIDC lipsește din URL.";
      }
    },
  );
