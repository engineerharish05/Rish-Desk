/* Runs in <head> before first paint: marks JS as available and restores the saved theme (no flash). */
(function () {
  var d = document.documentElement;
  d.classList.add("js");
  try {
    var t = localStorage.getItem("theme");
    if (t === "light" || t === "dark") d.setAttribute("data-theme", t);
  } catch (e) { /* storage blocked: follow the system theme */ }
})();
