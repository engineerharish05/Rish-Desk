/* Shared behaviour: mobile menu, theme toggle, header shadow, and (RishDesk page) latest-release links. */
(function () {
  var d = document.documentElement;

  /* Theme toggle */
  var themeBtn = document.getElementById("theme-btn");
  function isLight() {
    var t = d.getAttribute("data-theme");
    if (t) return t === "light";
    return window.matchMedia && window.matchMedia("(prefers-color-scheme: light)").matches;
  }
  if (themeBtn) {
    themeBtn.addEventListener("click", function () {
      var next = isLight() ? "dark" : "light";
      d.setAttribute("data-theme", next);
      try { localStorage.setItem("theme", next); } catch (e) {}
      themeBtn.setAttribute("aria-label", next === "light" ? "Switch to dark theme" : "Switch to light theme");
    });
    themeBtn.setAttribute("aria-label", isLight() ? "Switch to dark theme" : "Switch to light theme");
  }

  /* Mobile menu */
  var menuBtn = document.getElementById("menu-btn");
  var nav = document.getElementById("site-nav");
  if (menuBtn && nav) {
    function setOpen(open) {
      nav.classList.toggle("open", open);
      menuBtn.setAttribute("aria-expanded", open ? "true" : "false");
    }
    menuBtn.addEventListener("click", function () { setOpen(!nav.classList.contains("open")); });
    nav.addEventListener("click", function (e) { if (e.target.closest("a")) setOpen(false); });
    document.addEventListener("keydown", function (e) {
      if (e.key === "Escape" && nav.classList.contains("open")) { setOpen(false); menuBtn.focus(); }
    });
  }

  /* Header gets a bottom rule once the page has scrolled */
  var bar = document.querySelector(".bar");
  if (bar) {
    var onScroll = function () { bar.classList.toggle("scrolled", window.scrollY > 8); };
    window.addEventListener("scroll", onScroll, { passive: true });
    onScroll();
  }

  /* RishDesk page: read the newest release so the download buttons never go stale.
     If the request is blocked or fails, the static v1.1.0 links in the HTML stay as they are. */
  if (document.getElementById("apk")) {
    try {
      fetch("https://api.github.com/repos/engineerharish05/Rish-Desk/releases/latest", { headers: { Accept: "application/vnd.github+json" } })
        .then(function (r) { return r.ok ? r.json() : null; })
        .then(function (rel) {
          if (!rel || !rel.assets) return;
          var apk = rel.assets.find(function (a) { return /\.apk$/i.test(a.name); });
          var sha = rel.assets.find(function (a) { return /\.sha256$/i.test(a.name); });
          if (!apk) return;
          var mb = (apk.size / 1048576).toFixed(1) + " MB";
          var v = String(rel.tag_name || "");
          function set(id, fn) { var el = document.getElementById(id); if (el) fn(el); }
          ["apk", "apk2"].forEach(function (id) { set(id, function (el) { el.href = apk.browser_download_url; }); });
          if (sha) set("sha", function (el) { el.href = sha.browser_download_url; });
          set("ver", function (el) { el.textContent = v; });
          set("ver2", function (el) { el.textContent = v.replace(/^v/, ""); });
          set("size", function (el) { el.textContent = "About " + mb; });
          set("size2", function (el) { el.textContent = mb; });
        })
        .catch(function () {});
    } catch (e) {}
  }
})();
