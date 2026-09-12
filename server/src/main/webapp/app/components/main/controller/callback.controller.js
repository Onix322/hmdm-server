angular
  .module("headwind-kiosk")
  .controller("CallbackController", function ($scope, $state, authService) {
    // Reading natively from the URL before the '#' fragment
    var urlParams = new URLSearchParams(window.location.search);
    var authCode = urlParams.get("code");
    var state = urlParams.get("state");

    if (authCode) {
      console.log(authCode);
      console.log("we have the code");
    } else {
      $scope.errorMessage = "Authorization code not found in URL.";
    }
  });
