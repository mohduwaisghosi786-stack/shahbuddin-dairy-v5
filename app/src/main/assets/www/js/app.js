(function () {
  "use strict";

  var DB_KEY = "shahbuddindairy_db_v4";
  var SET_KEY = "shahbuddindairy_settings_v4";
  var SCHEMA = 4;
  var LEGACY_DB = ["shahbuddindairy_db_v3", "shahbuddindairy_db_v2", "shahbuddindairy_db_v1"];
  var LEGACY_SET = ["shahbuddindairy_settings_v3", "shahbuddindairy_settings_v2", "shahbuddindairy_settings_v1"];

  var _n = 0;
  function uid(p) { _n++; return (p || "id") + "_" + Date.now().toString(36) + "_" + _n; }
  function pad(n) { return n < 10 ? "0" + n : "" + n; }
  function todayStr(d) { d = d || new Date(); return d.getFullYear() + "-" + pad(d.getMonth() + 1) + "-" + pad(d.getDate()); }
  function prevDayStr(s) {
    var p = parseDateParts(s);
    if (!p) p = new Date();
    p.setUTCDate(p.getUTCDate() - 1);
    return p.getUTCFullYear() + "-" + pad(p.getUTCMonth() + 1) + "-" + pad(p.getUTCDate());
  }
  function timeStr(d) {
    d = d || new Date();
    var h = d.getHours(), m = pad(d.getMinutes()), ap = h >= 12 ? "PM" : "AM", hh = h % 12;
    return (hh === 0 ? 12 : hh) + ":" + m + " " + ap;
  }
  function fmtDate(s) { if (!s) return ""; var p = String(s).slice(0, 10).split("-"); return p.length === 3 ? p[2] + "-" + p[1] + "-" + p[0] : s; }
  function fmtDateShort(s) { if (!s) return ""; var p = String(s).slice(0, 10).split("-"); return p.length === 3 ? p[2] + " " + ["Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"][parseInt(p[1],10)-1] + " " + p[0] : s; }
  function num(v, d) { v = parseFloat(v); return isNaN(v) ? (d || 0) : v; }
  function r2(v) { return Math.round(num(v) * 100) / 100; }
  function r0(v) { return Math.round(num(v)); }
  function money(v) { return "\u20B9" + r2(v).toLocaleString("en-IN"); }
  function money0(v) { return "\u20B9" + r0(v).toLocaleString("en-IN"); }
  function lit(v) { return r2(v).toFixed(2) + " L"; }
  function esc(s) {
    return String(s == null ? "" : s).replace(/[&<>"']/g, function (c) {
      return ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[c];
    });
  }
  function parseDateParts(s) {
    var p = String(s || "").slice(0, 10).split("-");
    return p.length === 3 ? new Date(Date.UTC(parseInt(p[0], 10), parseInt(p[1], 10) - 1, parseInt(p[2], 10))) : null;
  }
  function dayDiff(a, b) {
    var da = parseDateParts(a), db = parseDateParts(b);
    if (!da || !db) return 0;
    return Math.round((db - da) / 86400000);
  }
  function prevDayStr(s) {
    var p = String(s || todayStr()).slice(0, 10).split("-");
    if (p.length !== 3) return "";
    var d = new Date(Date.UTC(parseInt(p[0], 10), parseInt(p[1], 10) - 1, parseInt(p[2], 10)));
    d.setUTCDate(d.getUTCDate() - 1);
    var y = d.getUTCFullYear();
    var m = d.getUTCMonth() + 1;
    var day = d.getUTCDate();
    return y + "-" + (m < 10 ? "0" + m : m) + "-" + (day < 10 ? "0" + day : day);
  }
  function safeParse(s, fb) { try { var v = JSON.parse(s); return v == null ? fb : v; } catch (e) { return fb; } }
  function encode(s) { try { return btoa(unescape(encodeURIComponent(s))); } catch (e) { return ""; } }
  function checksum(str) {
    var h = 5381;
    for (var i = 0; i < str.length; i++) { h = ((h << 5) + h + str.charCodeAt(i)) | 0; }
    return (h >>> 0).toString(16);
  }

  /* ================= SINGLE SOURCE OF TRUTH — CALC ENGINE ================= */
  var Calc = {
    portionTotal: function (p) { return r2(num(p.liters) * num(p.rate)); },
    deliveryBill: function (portions) {
      var t = 0;
      (portions || []).forEach(function (p) { t += num(p.liters) * num(p.rate); });
      return r2(t);
    },
    deliveryLiters: function (portions) {
      var t = 0;
      (portions || []).forEach(function (p) { t += num(p.liters); });
      return r2(t);
    },
    cash: function (d) { return d && d.paid_cash != null ? num(d.paid_cash) : num(d && d.paid_amount); },
    online: function (d) { return d ? num(d.paid_online) : 0; },
    paid: function (d) { if (!d) return 0; return d.total_paid != null ? num(d.total_paid) : r2(Calc.cash(d) + Calc.online(d)); },
    newBalance: function (oldDue, todayBill, paidCash, paidOnline) {
      return r2(num(oldDue) + num(todayBill) - num(paidCash) - num(paidOnline));
    },
    financialBreakdown: function (cid, date, portions, paidCash, paidOnline) {
      var bill = Calc.deliveryBill(portions);
      var l = Calc.deliveryLiters(portions);
      var prevDue = Calc.prevDueBefore(cid, date);
      var payment = r2(num(paidCash) + num(paidOnline));
      var totalReq = r2(bill + prevDue);
      var net = r2(totalReq - payment);
      var remDue = net > 0 ? net : 0;
      var advance = net < 0 ? r2(-net) : 0;
      return {
        liters: l,
        bill: bill,
        prevDue: prevDue,
        payment: payment,
        totalRequired: totalReq,
        remainingDue: remDue,
        advance: advance
      };
    },
    customerBalance: function (cid) {
      var bill = 0, paid = 0;
      DB.deliveries.forEach(function (d) {
        if (d.customer_id !== cid || d.is_void) return;
        if (d.status === "delivered") bill += num(d.total_amount);
        paid += Calc.paid(d);
      });
      DB.payments.forEach(function (p) {
        if (p.customer_id !== cid || p.is_void) return;
        paid += num(p.amount);
      });
      return r2(bill - paid);
    },
    prevDueBefore: function (cid, date) {
      var bill = 0, paid = 0;
      DB.deliveries.forEach(function (d) {
        if (d.customer_id !== cid || d.date >= date || d.is_void) return;
        if (d.status === "delivered") bill += num(d.total_amount);
        paid += Calc.paid(d);
      });
      DB.payments.forEach(function (p) {
        if (p.customer_id !== cid || p.date >= date || p.is_void) return;
        paid += num(p.amount);
      });
      return r2(bill - paid);
    },
    customerSummary: function (c) {
      var days = 0, l = 0, naaga = 0, bill = 0, cash = 0, online = 0;
      DB.deliveries.forEach(function (d) {
        if (d.customer_id !== c.id || d.is_void) return;
        if (d.status === "delivered") { days++; l += num(d.liters); bill += num(d.total_amount); }
        else if (d.status === "skipped") naaga++;
        cash += Calc.cash(d); online += Calc.online(d);
      });
      DB.payments.forEach(function (p) {
        if (p.customer_id !== c.id || p.is_void) return;
        if (p.payment_mode === "Cash") cash += num(p.amount); else online += num(p.amount);
      });
      var paid = r2(cash + online);
      return { days: days, liters: r2(l), naaga: naaga, bill: r2(bill), cash: r2(cash), online: r2(online), paid: paid, due: r2(bill - paid) };
    },
    marketDebt: function () {
      var t = 0;
      DB.customers.forEach(function (c) { t += Calc.customerBalance(c.id); });
      return r2(t);
    },
    dailySummary: function (date) {
      var k = { soldL: 0, buffalo: 0, cow: 0, bill: 0, cash: 0, online: 0, paid: 0, credit: 0,
                served: 0, naaga: 0, pending: 0, off: 0, cashCust: 0, monthlyCust: 0, total: 0, due: 0 };
      DB.customers.forEach(function (c) {
        if (num(c.is_active, 1) === 0 || c.is_archived) return;
        if (isOffDay(c, date)) { k.off++; return; }
        k.total++;
        if (c.billing_type === "daily_cash") k.cashCust++; else k.monthlyCust++;
        var d = deliveryFor(c.id, date);
        if (d && !d.is_void && d.status === "delivered") {
          k.served++; k.soldL += num(d.liters); k.bill += num(d.total_amount);
          k.cash += Calc.cash(d); k.online += Calc.online(d); k.paid += Calc.paid(d);
          k.credit += num(d.total_amount) - Calc.paid(d);
          safeParse(d.portions_json, []).forEach(function (p) {
            if (p.milk_type === "गाय") k.cow += num(p.liters); else k.buffalo += num(p.liters);
          });
        } else if (d && !d.is_void && d.status === "skipped") k.naaga++;
        else k.pending++;
      });
      // Also include direct payments recorded on this date in Khata
      DB.payments.forEach(function (p) {
        if (p.date === date && !p.is_void) {
          if (p.payment_mode === "Cash") k.cash += num(p.amount);
          else k.online += num(p.amount);
          k.paid += num(p.amount);
        }
      });
      k.soldL = r2(k.soldL); k.buffalo = r2(k.buffalo); k.cow = r2(k.cow);
      k.bill = r2(k.bill); k.cash = r2(k.cash); k.online = r2(k.online);
      k.paid = r2(k.paid); k.credit = r2(k.credit);
      return k;
    },
    routeProgress: function () {
      var date = todayStr();
      var list = routeList(date);
      var done = 0;
      list.forEach(function (c) {
        var d = deliveryFor(c.id, date);
        if (d && !d.is_void && (d.status === "delivered" || d.status === "skipped")) done++;
      });
      return { done: done, total: list.length, pct: list.length ? Math.round((done / list.length) * 100) : 0 };
    }
  };

  /* ================= STORAGE + MIGRATION ================= */
  function emptyDB() { return { schema: SCHEMA, customers: [], deliveries: [], payments: [], procurement: [] }; }

  function normalizeCustomer(c, i) {
    return {
      id: num(c.id, i + 1),
      name: c.name || "",
      phone: c.phone || "",
      mohalla: c.mohalla || "कटरा",
      landmark: c.landmark || "",
      note: c.note || "",
      milk_type: c.milk_type || "भैंस",
      default_liters: num(c.default_liters, 1),
      rate_per_liter: num(c.rate_per_liter, 0),
      billing_type: c.billing_type === "daily_cash" ? "daily_cash" : "monthly",
      delivery_frequency: c.delivery_frequency === "alternate" ? "alternate" : "daily",
      frequency_anchor_date: (c.frequency_anchor_date || c.created_at || "").slice(0, 10) || todayStr(),
      is_active: num(c.is_active, 1) === 0 ? 0 : 1,
      is_archived: num(c.is_archived, 0) === 1 ? 1 : 0,
      route_order: num(c.route_order, i + 1),
      portions_json: c.portions_json || "[]",
      created_at: c.created_at || new Date().toISOString()
    };
  }
  function normalizeDelivery(d) {
    var cash = d.paid_cash != null ? num(d.paid_cash) : num(d.paid_amount);
    var online = num(d.paid_online);
    return {
      id: d.id || uid("d"),
      customer_id: num(d.customer_id),
      date: String(d.date || "").slice(0, 10),
      delivery_time: d.delivery_time || "",
      milk_type: d.milk_type || "भैंस",
      liters: num(d.liters),
      rate: num(d.rate),
      total_amount: num(d.total_amount),
      paid_cash: cash,
      paid_online: online,
      total_paid: d.total_paid != null ? num(d.total_paid) : r2(cash + online),
      status: d.status === "skipped" ? "skipped" : "delivered",
      portions_json: d.portions_json || "[]",
      note: d.note || "",
      is_void: d.is_void === true,
      voided_at: d.voided_at || null
    };
  }
  function normalizePayment(p) {
    var mode = p.payment_mode || "Cash";
    return {
      id: p.id || uid("pay"),
      customer_id: num(p.customer_id),
      date: String(p.date || "").slice(0, 10),
      amount: num(p.amount),
      payment_mode: mode,
      payment_subtype: p.payment_subtype || (mode === "Cash" ? "" : mode),
      note: p.note || "Payment",
      created_at: p.created_at || new Date().toISOString(),
      is_void: p.is_void === true,
      voided_at: p.voided_at || null
    };
  }
  function migrateFrom(raw, tag) {
    var src = safeParse(raw, null);
    if (!src || typeof src !== "object") return null;
    var out = emptyDB();
    out.migrated_from = tag;
    out.migrated_at = new Date().toISOString();
    (src.customers || []).forEach(function (c, i) { out.customers.push(normalizeCustomer(c, i)); });
    (src.deliveries || []).forEach(function (d) { var n = normalizeDelivery(d); if (n.customer_id) out.deliveries.push(n); });
    (src.payments || []).forEach(function (p) { var n = normalizePayment(p); if (n.customer_id) out.payments.push(n); });
    out.procurement = (src.procurement || src.daily_procurement || []).slice();
    return out;
  }
  function loadDB() {
    try {
      var raw = localStorage.getItem(DB_KEY);
      if (raw) {
        var d = safeParse(raw, null);
        if (d && Array.isArray(d.customers)) { d.schema = SCHEMA; return d; }
      }
      for (var i = 0; i < LEGACY_DB.length; i++) {
        var lraw = localStorage.getItem(LEGACY_DB[i]);
        if (!lraw) continue;
        var migrated = migrateFrom(lraw, LEGACY_DB[i]);
        if (migrated) {
          localStorage.setItem(DB_KEY, JSON.stringify(migrated));
          console.log("Migrated data from " + LEGACY_DB[i] + ": " + migrated.customers.length + " customers");
          return migrated;
        }
      }
      return emptyDB();
    } catch (e) { return emptyDB(); }
  }
  function saveDB(db) { try { db.schema = SCHEMA; localStorage.setItem(DB_KEY, JSON.stringify(db)); } catch (e) { toast("Data not saved", "err"); } }
  function loadSettings() {
    try {
      var raw = localStorage.getItem(SET_KEY);
      if (raw) return JSON.parse(raw);
      for (var i = 0; i < LEGACY_SET.length; i++) {
        var l = localStorage.getItem(LEGACY_SET[i]);
        if (l) { var s = JSON.parse(l) || {}; localStorage.setItem(SET_KEY, JSON.stringify(s)); return s; }
      }
    } catch (e) { }
    return {};
  }
  function saveSettings(s) { try { localStorage.setItem(SET_KEY, JSON.stringify(s)); } catch (e) { } }

  var DB = loadDB();
  var SETTINGS = loadSettings();
  if (!SETTINGS.upi) SETTINGS.upi = "";
  if (!SETTINGS.payee) SETTINGS.payee = "Shahbuddin Dairy";
  if (!SETTINGS.phone) SETTINGS.phone = "";
  saveSettings(SETTINGS);

  /* ================= LOOKUPS ================= */
  var _custIndex = null;
  function reindexCustomers() {
    _custIndex = {};
    DB.customers.forEach(function (c) { _custIndex[c.id] = c; });
  }
  function getCustomer(id) {
    if (!_custIndex) reindexCustomers();
    return _custIndex[id] || null;
  }
  function nextCustomerId() {
    var max = 0; DB.customers.forEach(function (c) { if (c.id > max) max = c.id; }); return max + 1;
  }
  function portionsOf(c) {
    if (!c || !c.portions_json) return [];
    var p = safeParse(c.portions_json, []);
    return Array.isArray(p) ? p : [];
  }
  function effectivePortions(c) {
    var p = portionsOf(c);
    if (p.length) return p;
    return [{ id: "base", label: "Milk", liters: num(c.default_liters, 1), rate: num(c.rate_per_liter, 0), milk_type: c.milk_type || "भैंस" }];
  }
  function sortedCustomers() {
    return DB.customers.slice().sort(function (a, b) {
      return (num(a.route_order, 0) - num(b.route_order, 0)) || (a.id - b.id);
    });
  }
  function reindexRoute() {
    sortedCustomers().forEach(function (c, i) { c.route_order = i + 1; });
    saveDB(DB);
  }
  function deliveryFor(cid, date) {
    for (var i = 0; i < DB.deliveries.length; i++) {
      var d = DB.deliveries[i];
      if (d.customer_id === cid && d.date === date && !d.is_void) return d;
    }
    return null;
  }
  function isOffDay(c, date) {
    if (num(c.is_active, 1) === 0 || c.is_archived) return false;
    if (c.delivery_frequency !== "alternate") return false;
    var anchor = (c.frequency_anchor_date || date).slice(0, 10);
    var diff = dayDiff(anchor, date);
    if (diff < 0) diff = -diff;
    return diff % 2 !== 0;
  }
  function routeList(date) {
    date = date || todayStr();
    return sortedCustomers().filter(function (c) {
      return num(c.is_active, 1) === 1 && !c.is_archived && !isOffDay(c, date);
    });
  }
  function findNextPending(fromIndex) {
    var date = todayStr();
    var list = routeList(date);
    for (var i = fromIndex + 1; i < list.length; i++) {
      var d = deliveryFor(list[i].id, date);
      if (!d || (d.status !== "delivered" && d.status !== "skipped")) return i;
    }
    for (var j = 0; j < list.length; j++) {
      var d2 = deliveryFor(list[j].id, date);
      if (!d2 || (d2.status !== "delivered" && d2.status !== "skipped")) return j;
    }
    return -1;
  }

  /* ================= UI HELPERS ================= */
  function speak(text) {
    try {
      if (!("speechSynthesis" in window)) return;
      window.speechSynthesis.cancel();
      var u = new SpeechSynthesisUtterance(text);
      u.lang = "hi-IN"; u.rate = 0.95; u.pitch = 1;
      window.speechSynthesis.speak(u);
    } catch (e) { }
  }
  function toast(msg, cls, ms) {
    var el = document.getElementById("toast");
    el.textContent = msg;
    el.className = "toast show" + (cls ? " " + cls : "");
    clearTimeout(el._t);
    el._t = setTimeout(function () { el.className = "toast"; }, ms || 2200);
  }
  function kpi(v, l, cls) {
    return '<div class="kpi ' + (cls || "") + '"><b>' + esc(v) + '</b><span>' + esc(l) + '</span></div>';
  }
  function openSheet(id) { document.getElementById("backdrop").classList.add("show"); document.getElementById(id).classList.add("show"); }
  function closeSheet(id) {
    document.getElementById(id).classList.remove("show");
    if (!document.querySelector(".sheet.show") && !document.querySelector(".drawer.show")) {
      document.getElementById("backdrop").classList.remove("show");
    }
  }
  function closeAllSheets() {
    document.querySelectorAll(".sheet.show,.drawer.show").forEach(function (s) { s.classList.remove("show"); });
    document.getElementById("backdrop").classList.remove("show");
  }
  function downloadUri(uri, filename) {
    var a = document.createElement("a");
    a.href = uri; a.download = filename;
    document.body.appendChild(a); a.click(); document.body.removeChild(a);
  }

  window.__SHD = { kpi: kpi, Calc: Calc, get: function () { return DB; }, set: function (d) { DB = d; reindexCustomers(); saveDB(DB); }, getCustomer: getCustomer, deliveryFor: deliveryFor, effectivePortions: effectivePortions, isOffDay: isOffDay, routeList: routeList, sorted: sortedCustomers, settings: function () { return SETTINGS; }, setSettings: function (s) { SETTINGS = s; saveSettings(s); }, toast: toast, speak: speak, uid: uid, money: money, money0: money0, lit: lit, esc: esc, num: num, r2: r2, todayStr: todayStr, timeStr: timeStr, fmtDate: fmtDate, fmtDateShort: fmtDateShort, saveDB: saveDB, reindexRoute: reindexRoute, nextCustomerId: nextCustomerId, portionsOf: portionsOf, downloadUri: downloadUri, findNextPending: findNextPending, openSheet: openSheet, closeSheet: closeSheet, closeAll: closeAllSheets, encode: encode, checksum: checksum, normalizeCustomer: normalizeCustomer, normalizeDelivery: normalizeDelivery, normalizePayment: normalizePayment, prevDayStr: prevDayStr };
})();

/* ================= MAIN APP ================= */
(function () {
  "use strict";
  var S = window.__SHD;
  var Calc = S.Calc;
  var kpi = S.kpi, money = S.money, esc = S.esc, num = S.num, r2 = S.r2, uid = S.uid, toast = S.toast, prevDayStr = S.prevDayStr;
  /* ================= BACKUP & RECOVERY SYSTEM ================= */
  var BackupSystem = {
    SAFETY_BACKUP_KEY: "shahbuddindairy_safety_backup",
    META_KEY: "shahbuddindairy_backup_meta",
    _pendingRestore: null,

    getMeta: function () {
      try { return JSON.parse(localStorage.getItem(this.META_KEY)) || {}; } catch (e) { return {}; }
    },

    saveMeta: function (m) {
      try { localStorage.setItem(this.META_KEY, JSON.stringify(m)); } catch (e) {}
    },

    createPayload: function (isSafety) {
      var db = S.get();
      var st = S.settings();
      var payload = {
        app: "ShahbuddinDairy",
        app_version: "5.0.0",
        backup_format: 2,
        schema: 5,
        is_safety_snapshot: !!isSafety,
        created_at: new Date().toISOString(),
        manifest: {
          customers: (db.customers || []).length,
          deliveries: (db.deliveries || []).length,
          payments: (db.payments || []).length,
          procurement: (db.procurement || []).length
        },
        database: {
          customers: db.customers || [],
          deliveries: db.deliveries || [],
          payments: db.payments || [],
          procurement: db.procurement || []
        },
        settings: st,
        checksum: ""
      };
      payload.checksum = S.checksum(JSON.stringify(payload.database) + JSON.stringify(payload.settings));
      return payload;
    },

    exportBackup: function () {
      try {
        var payload = this.createPayload(false);
        var str = JSON.stringify(payload, null, 2);
        var now = new Date();
        var timeSuffix = (now.getHours() < 10 ? "0" : "") + now.getHours() + (now.getMinutes() < 10 ? "0" : "") + now.getMinutes();
        var filename = "ShahbuddinDairy_Backup_" + S.todayStr().replace(/-/g, "_") + "_" + timeSuffix + ".json";
        S.downloadUri("data:application/json;charset=utf-8;base64," + S.encode(str), filename);

        var m = this.getMeta();
        m.lastBackup = new Date().toISOString();
        m.backupCount = (m.backupCount || 0) + 1;
        this.saveMeta(m);

        toast("Backup created & saved to Downloads ✓", "ok");
        this.updateStatus();
      } catch (e) {
        toast("Error creating backup: " + e.message, "err");
      }
    },

    validate: function (rawOrObj) {
      var data = rawOrObj;
      if (typeof data === "string") {
        try { data = JSON.parse(data); } catch (e) { return { valid: false, error: "Invalid JSON format" }; }
      }
      if (!data || typeof data !== "object") return { valid: false, error: "Empty or invalid backup data" };

      var db = null;
      var settings = data.settings || {};
      var format = data.backup_format || (data.manifest && data.manifest.backup_format) || 0;
      var schema = data.schema || (data.manifest && data.manifest.schema) || 3;

      if (data.database && Array.isArray(data.database.customers)) {
        db = data.database;
      } else if (Array.isArray(data.customers)) {
        db = {
          customers: data.customers,
          deliveries: data.deliveries || [],
          payments: data.payments || [],
          procurement: data.procurement || data.daily_procurement || []
        };
      }

      if (!db || !Array.isArray(db.customers)) {
        return { valid: false, error: "Backup does not contain valid customer records" };
      }

      var badCust = false;
      db.customers.forEach(function (c) {
        if (!c || !c.name) badCust = true;
      });
      if (badCust) {
        return { valid: false, error: "Incomplete or corrupted customer entries found in backup" };
      }

      var checksumOk = true;
      if (data.checksum) {
        var calcChecksum = S.checksum(JSON.stringify(db) + JSON.stringify(settings));
        checksumOk = (calcChecksum === data.checksum);
      }

      var totalLit = 0, totalBill = 0, totalCash = 0, totalOnline = 0, totalPaid = 0;
      (db.deliveries || []).forEach(function (d) {
        if (d.status === "delivered" && !d.is_void) {
          totalLit += num(d.liters, 0);
          totalBill += num(d.total_amount, 0);
        }
        if (!d.is_void) {
          var c = d.paid_cash != null ? num(d.paid_cash) : num(d.paid_amount);
          var o = num(d.paid_online, 0);
          totalCash += c;
          totalOnline += o;
          totalPaid += (c + o);
        }
      });
      (db.payments || []).forEach(function (p) {
        if (!p.is_void) {
          var a = num(p.amount, 0);
          if (p.payment_mode === "Cash") totalCash += a;
          else totalOnline += a;
          totalPaid += a;
        }
      });

      var onlinePaymentsCount = 0;
      (db.deliveries || []).forEach(function (d) { if (num(d.paid_online, 0) > 0) onlinePaymentsCount++; });
      (db.payments || []).forEach(function (p) { if (p.payment_mode !== "Cash" && !p.is_adjustment) onlinePaymentsCount++; });

      return {
        valid: true,
        format: format,
        schema: schema,
        checksumOk: checksumOk,
        stats: {
          customers: db.customers.length,
          deliveries: (db.deliveries || []).length,
          payments: (db.payments || []).length,
          onlinePaymentsCount: onlinePaymentsCount,
          liters: r2(totalLit),
          bill: r2(totalBill),
          paid: r2(totalPaid),
          cash: r2(totalCash),
          online: r2(totalOnline)
        },
        db: db,
        settings: settings,
        created_at: data.created_at || (data.manifest && data.manifest.created_at) || null
      };
    },

    onVerifyFileInput: function (e) {
      var file = e.target.files && e.target.files[0];
      if (!file) return;
      var self = this;
      var reader = new FileReader();
      reader.onload = function (evt) {
        var res = self.validate(evt.target.result);
        var box = document.getElementById("backupResultBox");
        if (!box) return;
        if (!res.valid) {
          box.innerHTML = '<div style="color:var(--danger);font-weight:700">❌ Verification Failed: ' + esc(res.error) + '</div>';
          toast("Backup verification failed: " + res.error, "err");
        } else {
          box.innerHTML =
            '<div style="color:var(--success);font-weight:800">✅ Backup Verified Valid</div>' +
            '<div style="font-size:12px;margin-top:6px;line-height:1.5">' +
            '• File: ' + esc(file.name) + '<br>' +
            '• Date: ' + esc(res.created_at ? new Date(res.created_at).toLocaleString() : "Unknown") + '<br>' +
            '• Customers: <b>' + res.stats.customers + '</b><br>' +
            '• Milk Records: <b>' + res.stats.deliveries + '</b> (' + res.stats.liters + ' L)<br>' +
            '• Payments: <b>' + res.stats.payments + '</b> (₹' + res.stats.paid + ')<br>' +
            '• Historical Online Payments: <b>' + res.stats.onlinePaymentsCount + '</b><br>' +
            '• Checksum: ' + (res.checksumOk ? '<b style="color:var(--success)">Valid ✓</b>' : '<b style="color:var(--warning)">Warning: Mismatch</b>') +
            '</div>';
          toast("Backup file is valid ✓", "ok");
        }
        e.target.value = "";
      };
      reader.readAsText(file);
    },

    onRestoreFileInput: function (e) {
      var file = e.target.files && e.target.files[0];
      if (!file) return;
      var self = this;
      var reader = new FileReader();
      reader.onload = function (evt) {
        var res = self.validate(evt.target.result);
        if (!res.valid) {
          toast("Cannot restore: " + res.error, "err");
          e.target.value = "";
          return;
        }
        self._pendingRestore = res;
        var box = document.getElementById("restoreDetailsBox");
        if (box) {
          box.innerHTML =
            '<div class="sc-row"><span class="sc-lbl">📁 Backup File</span><span class="sc-val">' + esc(file.name) + '</span></div>' +
            '<div class="sc-row"><span class="sc-lbl">📅 Backup Created</span><span class="sc-val">' + esc(res.created_at ? new Date(res.created_at).toLocaleString() : "N/A") + '</span></div>' +
            '<div class="sc-row"><span class="sc-lbl">👥 Customers</span><span class="sc-val">' + res.stats.customers + '</span></div>' +
            '<div class="sc-row"><span class="sc-lbl">🥛 Milk Records</span><span class="sc-val">' + res.stats.deliveries + ' (' + res.stats.liters + ' L)</span></div>' +
            '<div class="sc-row"><span class="sc-lbl">💵 Payments</span><span class="sc-val">' + res.stats.payments + ' (₹' + res.stats.paid + ')</span></div>' +
            '<div class="sc-row"><span class="sc-lbl">📲 Online Payments</span><span class="sc-val">' + res.stats.onlinePaymentsCount + '</span></div>' +
            '<div class="sc-row"><span class="sc-lbl">🛡️ Integrity Check</span><span class="sc-val">' + (res.checksumOk ? "Passed ✓" : "Notice: Legacy format") + '</span></div>';
        }
        S.openSheet("restoreConfirmSheet");
        e.target.value = "";
      };
      reader.readAsText(file);
    },

    executeRestore: function () {
      if (!this._pendingRestore || !this._pendingRestore.valid) {
        toast("No valid backup to restore", "err");
        return;
      }
      var res = this._pendingRestore;
      var currentDb = null;
      try {
        currentDb = S.get();
        // 1. Create safety snapshot of current data before overwriting
        var safetySnapshot = {
          app: "ShahbuddinDairy",
          snapshot_reason: "pre_restore_safety",
          created_at: new Date().toISOString(),
          database: currentDb,
          settings: S.settings()
        };
        localStorage.setItem(this.SAFETY_BACKUP_KEY, JSON.stringify(safetySnapshot));

        // 2. Safely normalize all records
        var migratedCustomers = (res.db.customers || []).map(function (c, i) { return S.normalizeCustomer(c, i); });
        var migratedDeliveries = (res.db.deliveries || []).map(function (d) { return S.normalizeDelivery(d); });
        var migratedPayments = (res.db.payments || []).map(function (p) { return S.normalizePayment(p); });
        var migratedProcurement = (res.db.procurement || []).slice();

        var newDb = {
          schema: 4,
          customers: migratedCustomers,
          deliveries: migratedDeliveries,
          payments: migratedPayments,
          procurement: migratedProcurement
        };

        // 3. Save to database
        S.set(newDb);
        if (res.settings && Object.keys(res.settings).length) {
          S.setSettings(res.settings);
        }

        this._pendingRestore = null;
        S.closeSheet("restoreConfirmSheet");

        // 4. Update status & UI
        if (window.App && App.refreshAll) App.refreshAll();
        this.updateStatus();
        toast("Backup restored successfully ✓ (Safety snapshot created)", "ok", 4000);
      } catch (err) {
        // Rollback to previous live state
        if (currentDb) S.set(currentDb);
        toast("Restore failed: " + err.message + " — Database rolled back", "err", 5000);
      }
    },

    revertSafetyBackup: function () {
      var raw = localStorage.getItem(this.SAFETY_BACKUP_KEY);
      if (!raw) {
        toast("No safety snapshot available", "err");
        return;
      }
      if (!confirm("Revert database back to the safety snapshot taken before the last restore?\n\nCurrent changes will be replaced with that snapshot.")) return;
      try {
        var snapshot = JSON.parse(raw);
        if (!snapshot || !snapshot.database) {
          toast("Corrupt safety snapshot", "err");
          return;
        }
        S.set(snapshot.database);
        if (snapshot.settings) S.setSettings(snapshot.settings);
        if (window.App && App.refreshAll) App.refreshAll();
        this.updateStatus();
        toast("Reverted back to pre-restore safety snapshot ✓", "ok");
      } catch (e) {
        toast("Could not revert: " + e.message, "err");
      }
    },

    updateStatus: function () {
      var box = document.getElementById("backupStatusInfo");
      var revertBtn = document.getElementById("restoreSafetyBackupBtn");
      var db = S.get();
      var meta = this.getMeta();
      var lastBackupText = meta.lastBackup ? new Date(meta.lastBackup).toLocaleString() : "Never";
      var hasSafety = !!localStorage.getItem(this.SAFETY_BACKUP_KEY);

      if (box) {
        var onlinePaymentsCount = 0;
        (db.deliveries || []).forEach(function (d) { if (num(d.paid_online, 0) > 0) onlinePaymentsCount++; });
        (db.payments || []).forEach(function (p) { if (p.payment_mode !== "Cash" && !p.is_adjustment) onlinePaymentsCount++; });

        box.innerHTML =
          '<b>Last Backup:</b> ' + esc(lastBackupText) + '<br>' +
          '<b>Current Records:</b> ' + (db.customers || []).length + ' Customers • ' +
          (db.deliveries || []).length + ' Milk Records • ' +
          (db.payments || []).length + ' Payments (' + onlinePaymentsCount + ' Online)';
      }

      if (revertBtn) {
        revertBtn.style.display = hasSafety ? "block" : "none";
      }
    }
  };

  window.BackupSystem = BackupSystem;
  S.BackupSystem = BackupSystem;

  var App = {
    tab: "task",
    filter: { salePending: false, report: "all", cust: "active", search: "" },
    _builder: [],
    _khataId: null,
    _editId: null,
    _lastUndo: null,

    init: function () {
      this.bindHeader();
      this.bindNav();
      this.bindSheets();
      this.bindCustomerForm();
      this.bindAccounts();
      this.bindReports();
      Runner.init();
      document.getElementById("headerDate").textContent = S.fmtDateShort(S.todayStr());
      document.getElementById("reportDate").value = S.todayStr();
      var st = S.settings();
      document.getElementById("setUpi").value = st.upi || "";
      document.getElementById("setPayee").value = st.payee || "Shahbuddin Dairy";
      document.getElementById("setPhone").value = st.phone || "";
      this.refreshAll();
    },

    refreshAll: function () {
      Runner.refresh();
      this.renderSale();
      this.renderReport();
      this.renderCustomers();
      this.renderAccounts();
      this.updateDebt();
      this.updateNavDot();
    },

    updateDebt: function () {
      document.getElementById("marketDebt").textContent = money(Calc.marketDebt());
    },
    updateNavDot: function () {
      var date = S.todayStr();
      var list = S.routeList(date);
      var pending = 0;
      list.forEach(function (c) {
        var d = S.deliveryFor(c.id, date);
        if (!d || (d.status !== "delivered" && d.status !== "skipped")) pending++;
      });
      var dot = document.getElementById("navDotTask");
      if (pending > 0) { dot.textContent = pending > 99 ? "99+" : pending; dot.style.display = "flex"; }
      else dot.style.display = "none";
    },

    switchTab: function (t) {
      this.tab = t;
      document.querySelectorAll(".view").forEach(function (v) { v.classList.remove("active"); });
      var el = document.getElementById("view-" + t);
      if (el) el.classList.add("active");
      document.querySelectorAll(".nav-btn").forEach(function (b) {
        b.classList.toggle("active", b.getAttribute("data-tab") === t);
      });
      var header = document.getElementById("appHeader");
      header.classList.toggle("compact", t === "task");
      if (t === "task") Runner.refresh();
      if (t === "sale") this.renderSale();
      if (t === "report") this.renderReport();
      if (t === "customers") this.renderCustomers();
      if (t === "accounts") this.renderAccounts();
      window.scrollTo(0, 0);
    },

    bindNav: function () {
      var self = this;
      document.querySelectorAll(".nav-btn").forEach(function (b) {
        b.addEventListener("click", function () { self.switchTab(b.getAttribute("data-tab")); });
      });
    },

    bindHeader: function () {
      var self = this;
      var inp = document.getElementById("searchInput");
      var t;
      inp.addEventListener("input", function () {
        clearTimeout(t);
        t = setTimeout(function () {
          self.filter.search = inp.value.trim().toLowerCase();
          self.renderSale(); self.renderCustomers();
        }, 180);
      });
      document.getElementById("micBtn").addEventListener("click", function () { self.voiceSearch(); });
    },

    voiceSearch: function () {
      var SR = window.SpeechRecognition || window.webkitSpeechRecognition;
      if (!SR) { toast("Voice search not available on this device", "err"); return; }
      var self = this;
      try {
        var r = new SR();
        r.lang = "hi-IN"; r.interimResults = false; r.maxAlternatives = 1;
        r.onresult = function (e) {
          var txt = (e.results[0][0].transcript || "").trim();
          document.getElementById("searchInput").value = txt;
          self.filter.search = txt.toLowerCase();
          self.renderSale(); self.renderCustomers();
          toast("Search: " + txt, "ok");
        };
        r.onerror = function () { toast("Could not understand voice", "err"); };
        r.start();
        toast("Speak now...");
      } catch (e) { toast("Microphone not available", "err"); }
    },

    matchSearch: function () {
      var q = this.filter.search;
      if (!q) return function () { return true; };
      return function (c) {
        return ((c.name + " " + (c.phone || "") + " " + (c.mohalla || "") + " " + (c.landmark || "")).toLowerCase().indexOf(q) !== -1);
      };
    },

    bindSheets: function () {
      var self = this;
      document.querySelectorAll("[data-close]").forEach(function (b) {
        b.addEventListener("click", function () { S.closeSheet(b.getAttribute("data-close")); });
      });
      document.getElementById("backdrop").addEventListener("click", S.closeAll);

      document.getElementById("closeDrawerBtn").addEventListener("click", function () {
        document.getElementById("routeDrawer").classList.remove("show");
        document.getElementById("backdrop").classList.remove("show");
      });
      document.getElementById("drawerSearch").addEventListener("input", function () { self.renderDrawer(); });
      document.getElementById("nextPendingBtn").addEventListener("click", function () {
        var idx = S.findNextPending(Runner.idx);
        if (idx >= 0) { Runner.jumpTo(idx); document.getElementById("routeDrawer").classList.remove("show"); document.getElementById("backdrop").classList.remove("show"); }
        else toast("All customers done! 🎉", "ok");
      });

      document.getElementById("savePaymentBtn").addEventListener("click", function () { self.savePayment(); });
      document.getElementById("saveAdhocBtn").addEventListener("click", function () { self.saveAdhoc(); });
      document.getElementById("khataCashBtn").addEventListener("click", function () { self.openPayment("Cash"); });
      document.getElementById("khataOnlineBtn").addEventListener("click", function () { self.openPayment("UPI"); });
      document.getElementById("khataWaBtn").addEventListener("click", function () { self.waBill(); });
      document.getElementById("khataUpiBtn").addEventListener("click", function () { self.upiPay(); });
      var adjBtn = document.getElementById("khataAdjBtn");
      if (adjBtn) adjBtn.addEventListener("click", function () { self.openAdj(); });
      var saveAdjBtn = document.getElementById("saveAdjBtn");
      if (saveAdjBtn) saveAdjBtn.addEventListener("click", function () { self.saveAdj(); });
      document.getElementById("khataLedger").addEventListener("click", function (e) {
        var vb = e.target.closest("[data-voidentry]");
        if (vb) { self.voidEntry(vb.getAttribute("data-voidentry")); return; }
        var uv = e.target.closest("[data-unvoidentry]");
        if (uv) { self.unvoidEntry(uv.getAttribute("data-unvoidentry")); return; }
        var b = e.target.closest("[data-delentry]");
        if (b) { self.voidEntry(b.getAttribute("data-delentry")); return; }
      });
      var doRestoreBtn = document.getElementById("doRestoreBtn");
      if (doRestoreBtn) {
        doRestoreBtn.addEventListener("click", function () {
          if (window.BackupSystem) window.BackupSystem.executeRestore();
        });
      }

      document.getElementById("successOkBtn").addEventListener("click", function () {
        document.getElementById("successPop").classList.remove("show");
        Runner.next(true);
      });
      document.getElementById("successUndoBtn").addEventListener("click", function () {
        document.getElementById("successPop").classList.remove("show");
        self.undoLast();
      });
    },

    showSuccess: function (title, text, undoFn) {
      document.getElementById("successTitle").textContent = title;
      document.getElementById("successText").textContent = text;
      this._lastUndo = undoFn || null;
      document.getElementById("successUndoBtn").style.display = undoFn ? "inline-flex" : "none";
      document.getElementById("successPop").classList.add("show");
      clearTimeout(this._popT);
      var self = this;
      this._popT = setTimeout(function () {
        var p = document.getElementById("successPop");
        if (p.classList.contains("show")) { p.classList.remove("show"); Runner.next(true); }
      }, 2600);
    },

    undoLast: function () {
      if (this._lastUndo) { this._lastUndo(); this._lastUndo = null; this.refreshAll(); }
    },

    call: function (phone) {
      phone = (phone || "").replace(/[^0-9+]/g, "");
      if (!phone) { toast("Mobile number No is", "err"); return; }
      window.location.href = "tel:" + phone;
    },

    openRouteDrawer: function () { this.renderDrawer(); document.getElementById("routeDrawer").classList.add("show"); document.getElementById("backdrop").classList.add("show"); },

    renderDrawer: function () {
      var q = (document.getElementById("drawerSearch").value || "").trim().toLowerCase();
      var date = S.todayStr();
      var list = S.routeList(date);
      var el = document.getElementById("drawerList");
      el.innerHTML = "";
      list.forEach(function (c, i) {
        if (q && ((c.name + " " + (c.mohalla || "")).toLowerCase().indexOf(q) === -1)) return;
        var d = S.deliveryFor(c.id, date);
        var st = !d ? "Pending" : (d.status === "delivered" ? "✓" : "Naaga");
        var div = document.createElement("div");
        div.className = "ditem" + (i === Runner.idx ? " active" : "");
        div.innerHTML = '<div class="ditem-l"><span class="ditem-pos">' + (i + 1) + '</span><span class="ditem-name">' + esc(c.name) + '</span></div><span class="badge ' + (!d ? "off" : (d.status === "delivered" ? "done" : "paused")) + '">' + st + '</span>';
        div.addEventListener("click", function () {
          Runner.jumpTo(i);
          document.getElementById("routeDrawer").classList.remove("show");
          document.getElementById("backdrop").classList.remove("show");
        });
        el.appendChild(div);
      });
    },

    /* ---------- SALE LIST ---------- */
    renderSale: function () {
      var self = this;
      var date = S.todayStr();
      document.getElementById("saleDateLabel").textContent = S.fmtDateShort(date);
      var list = S.sorted();
      var ms = this.matchSearch();
      var k = Calc.dailySummary(date);
      document.getElementById("saleKpis").innerHTML =
        kpi(k.served, "Done") + kpi(k.pending, "Pending") + kpi(k.naaga, "Naaga") +
        kpi(r2(k.soldL).toFixed(2), "Liters") + kpi(money(k.bill), "Bill") + kpi(money(k.paid), "Collected");

      var cont = document.getElementById("saleList");
      cont.innerHTML = "";
      var shown = list.filter(function (c) {
        if (num(c.is_active, 1) === 0) return false;
        if (!ms(c)) return false;
        if (self.filter.salePending) {
          if (S.isOffDay(c, date)) return false;
          var d = S.deliveryFor(c.id, date);
          return !d || (d.status !== "delivered" && d.status !== "skipped");
        }
        return true;
      });
      if (!shown.length) {
        cont.innerHTML = '<div class="empty"><div class="empty-ico">✅</div><h3>Nothing</h3><p>Change filter or add customers.</p></div>';
        return;
      }
      shown.forEach(function (c) { cont.appendChild(self.saleCard(c, date)); });
    },

    saleCard: function (c, date) {
      var self = this;
      var d = S.deliveryFor(c.id, date);
      var off = S.isOffDay(c, date);
      var status = !d ? "pending" : (d.status === "delivered" ? "delivered" : "skipped");
      if (off) status = "paused";
      var due = Calc.customerBalance(c.id);
      var card = document.createElement("div");
      card.className = "opcard " + status;
      var pos = num(c.route_order, 0);
      var head = '<div class="opcard-top"><div style="display:flex;gap:9px;min-width:0">' +
        '<span class="opcard-pos">' + (pos < 10 ? "0" + pos : pos) + '</span>' +
        '<div style="min-width:0"><div class="opcard-name" data-open="' + c.id + '">' + esc(c.name) + '</div>' +
        '<div class="opcard-meta">📍 ' + esc(c.landmark || c.mohalla || "-") + (c.phone ? ' · 📞 ' + esc(c.phone) : "") + '</div></div></div>' +
        '<div class="opcard-amt ' + (due > 0 ? "due" : "paid") + '">' + money(due) + '</div></div>';
      var line = "";
      if (d && d.status === "delivered") {
        line = '<div class="opcard-line"><span>✓ Done <b>' + num(d.liters).toFixed(2) + ' L</b></span><span>Bill <b>' + money(d.total_amount) + '</b></span>' +
          '<span>Cash <b>' + money(Calc.cash(d)) + '</b></span><span>UPI <b>' + money(Calc.online(d)) + '</b></span></div>';
      } else if (off) {
        line = '<div class="opcard-line"><span class="badge off">Scheduled Off today</span></div>';
      } else if (d && d.status === "skipped") {
        line = '<div class="opcard-line"><span class="badge paused">Naaga</span></div>';
      } else {
        var eff = S.effectivePortions(c);
        line = '<div class="opcard-line"><span>' + Calc.deliveryLiters(eff).toFixed(2) + ' L</span><span>' + num(c.rate_per_liter) + ' ₹/L</span><span>' + (c.billing_type === "daily_cash" ? "Daily Cash" : "Monthly") + '</span></div>';
      }
      card.innerHTML = head + line;
      var act = document.createElement("div");
      act.className = "opcard-act";
      if (d && d.status === "delivered") {
        act.innerHTML = '<button class="opbtn ok" disabled>✓ Done</button><button class="opbtn naaga" data-naaga="' + c.id + '">Naaga</button><button class="opbtn open" data-open="' + c.id + '">Open</button>';
      } else if (off) {
        act.innerHTML = '<button class="opbtn ok" data-serve="' + c.id + '">+ Deliver Today</button>';
      } else {
        act.innerHTML = '<button class="opbtn ok" data-give="' + c.id + '">✓ Done</button><button class="opbtn naaga" data-naaga="' + c.id + '">Naaga</button><button class="opbtn open" data-open="' + c.id + '">Open</button>';
      }
      card.appendChild(act);
      return card;
    },

    bindSaleClicks: function () {
      var self = this;
      document.getElementById("saleList").addEventListener("click", function (e) {
        var t = e.target.closest("[data-give],[data-naaga],[data-open],[data-serve]");
        if (!t) return;
        var date = S.todayStr();
        if (t.hasAttribute("data-open")) { self.openKhata(parseInt(t.getAttribute("data-open"), 10)); return; }
        var id = parseInt(t.getAttribute("data-give") || t.getAttribute("data-naaga") || t.getAttribute("data-serve"), 10);
        var c = S.getCustomer(id); if (!c) return;
        if (t.hasAttribute("data-give") || t.hasAttribute("data-serve")) { Runner.commitFromList(c, date); }
        else if (t.hasAttribute("data-naaga")) { Runner.naagaFromList(c, date); }
      });
      document.getElementById("salePendingBtn").addEventListener("click", function () {
        self.filter.salePending = !self.filter.salePending;
        this.textContent = self.filter.salePending ? "All Show" : "Only Pending";
        this.classList.toggle("btn-primary", self.filter.salePending);
        self.renderSale();
      });
    },

    /* ---------- REPORTS ---------- */
    renderReport: function () {
      var date = document.getElementById("reportDate").value || S.todayStr();
      var k = Calc.dailySummary(date);
      var prog = Calc.routeProgress();
      document.getElementById("reportKpis").innerHTML =
        kpi(k.soldL.toFixed(2) + " L", "Milk Sale") +
        kpi(money(k.bill), "Total Bill") +
        kpi(money(k.cash), "Cash", "success") +
        kpi(money(k.online), "UPI / Online") +
        kpi(money(k.credit), "New Due", k.credit > 0 ? "danger" : "success") +
        kpi(k.served + " / " + k.naaga + " / " + k.pending, "Done/Naaga/Pending") +
        kpi(k.buffalo.toFixed(2) + " L", "भैंस") +
        kpi(k.cow.toFixed(2) + " L", "गाय");
      document.getElementById("reportKpis2").innerHTML =
        kpi(k.monthlyCust, "Monthly Account") +
        kpi(k.cashCust, "Daily Cash") +
        kpi(k.total, "Total Route") +
        kpi(prog.pct + "%", "Route Complete");

      var list = S.sorted();
      var f = this.filter.report;
      var cont = document.getElementById("reportList");
      cont.innerHTML = "";
      var shown = list.filter(function (c) { return f === "all" || c.billing_type === f; });
      if (!shown.length) { cont.innerHTML = '<div class="empty"><div class="empty-ico">📊</div><h3>No records</h3></div>'; return; }
      shown.forEach(function (c) {
        var d = S.deliveryFor(c.id, date);
        var card = document.createElement("div");
        card.className = "opcard";
        var st = !d ? '<span class="badge off">Pending</span>' : (d.status === "delivered" ? '<span class="badge done">✓ Done</span>' : '<span class="badge paused">Naaga</span>');
        card.innerHTML = '<div class="opcard-top"><div style="min-width:0"><div class="opcard-name">' + esc(c.name) + '</div>' +
          '<div class="opcard-meta"><span>📍 ' + esc(c.mohalla || "-") + '</span><span>' + (c.billing_type === "daily_cash" ? "Daily Cash" : "Monthly") + '</span>' +
          (c.delivery_frequency === "alternate" ? '<span>Alternate</span>' : "") + '</div></div>' + st + '</div>' +
          (d && d.status === "delivered" ? '<div class="opcard-line"><span>🕒 ' + esc(d.delivery_time || "") + '</span><span><b>' + num(d.liters).toFixed(2) + ' L</b></span><span>Bill <b>' + money(d.total_amount) + '</b></span><span>Cash ' + money(Calc.cash(d)) + '</span><span>UPI ' + money(Calc.online(d)) + '</span></div>' : "");
        cont.appendChild(card);
      });
    },

    bindReports: function () {
      var self = this;
      document.getElementById("reportDate").addEventListener("change", function () { self.renderReport(); });
      document.getElementById("reportFilter").addEventListener("click", function (e) {
        var b = e.target.closest(".fseg"); if (!b) return;
        self.filter.report = b.getAttribute("data-filter");
        document.querySelectorAll("#reportFilter .fseg").forEach(function (x) { x.classList.remove("active"); });
        b.classList.add("active"); self.renderReport();
      });
      document.getElementById("pdfBtn").addEventListener("click", function () { Pdf.make(); });
      document.getElementById("reportExcelBtn").addEventListener("click", function () { self.exportExcel(); });
      document.getElementById("waReportBtn").addEventListener("click", function () { self.waReport(); });
      var crb = document.getElementById("copyReportBtn");
      if (crb) crb.addEventListener("click", function () { self.copyReport(); });
    },

    buildReportText: function (date) {
      var k = Calc.dailySummary(date);
      var rows = [];
      S.routeList(date).forEach(function (c, i) {
        var d = S.deliveryFor(c.id, date);
        var lit = d && d.status === "delivered" ? num(d.liters) : 0;
        var bill = d && d.status === "delivered" ? num(d.total_amount) : 0;
        var cash = d ? Calc.cash(d) : 0;
        var online = d ? Calc.online(d) : 0;
        var paid = r2(cash + online);
        var oldDue = Calc.prevDueBefore(c.id, date);
        rows.push({ n: i + 1, c: c, d: d, lit: lit, bill: bill, cash: cash, online: online, paid: paid, oldDue: oldDue, newBal: r2(oldDue + bill - paid) });
      });
      var L = [];
      L.push("*SHAHBUDDIN DAIRY*");
      L.push("*Daily Milk Report*");
      L.push("\uD83D\uDCC5 " + S.fmtDate(date));
      L.push("");
      rows.forEach(function (r) {
        L.push("\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501");
        var no = (r.n < 10 ? "0" + r.n : "" + r.n);
        L.push("\uD83D\uDC64 *" + no + ". " + r.c.name + "*");
        L.push("\uD83D\uDCCD " + (r.c.landmark || r.c.mohalla || "-"));
        if (r.d && r.d.status === "delivered") {
          L.push("\uD83D\uDD50 " + (r.d.delivery_time || ""));
          L.push("");
          if (r.d.rate) L.push("\uD83E\uDD5B " + r.lit.toFixed(2) + " L \u00D7 \u20B9" + num(r.d.rate) + " = *" + money(r.bill) + "*");
          else L.push("\uD83E\uDD5B " + r.lit.toFixed(2) + " L = *" + money(r.bill) + "*");
          L.push("\uD83D\uDCB5 Cash " + money(r.cash) + "  \u2022  \uD83D\uDCF2 UPI " + money(r.online));
          L.push("\uD83D\uDCB3 Paid *" + money(r.paid) + "*  \u2022  " + (r.newBal > 0 ? "\uD83D\uDD34 Due *" + money(r.newBal) + "*" : "\u2705 Due *\u20B90*"));
        } else if (r.d && r.d.status === "skipped") {
          L.push("");
          L.push("\u274C *Naaga*");
        } else if (S.isOffDay(r.c, date)) {
          L.push("");
          L.push("\u23F8 *Scheduled Off*");
        } else {
          L.push("");
          L.push("\u23F3 *Pending*");
        }
      });
      L.push("\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501");
      L.push("*Daily Closing*");
      L.push("\uD83E\uDD5B Milk " + k.soldL.toFixed(2) + " L");
      L.push("\uD83E\uDDFE Bills " + money(k.bill));
      L.push("\uD83D\uDCB5 Cash " + money(k.cash));
      L.push("\uD83D\uDCF2 UPI " + money(k.online));
      L.push("\uD83D\uDCB3 Received " + money(k.paid));
      L.push("\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501");
      L.push("*Shahbuddin Dairy*");
      L.push("Gulab Nagar, Bareilly");
      return L.join("\n");
    },

    waReport: function () {
      var date = document.getElementById("reportDate").value || S.todayStr();
      var txt = this.buildReportText(date);
      var LIMIT = 3500;
      if (txt.length <= LIMIT) {
        window.location.href = "whatsapp://send?text=" + encodeURIComponent(txt);
        return;
      }
      var lines = txt.split("\n");
      var parts = [], cur = [];
      var header = lines.slice(0, 13);
      lines.slice(13).forEach(function (ln) {
        cur.push(ln);
        if (cur.join("\n").length > LIMIT) {
          var last = cur.pop();
          parts.push(cur.join("\n"));
          cur = [last];
        }
      });
      if (cur.length) parts.push(cur.join("\n"));
      if (parts.length > 1) {
        var total = parts.length;
        var all = parts.map(function (p, i) {
          return header.join("\n") + "\n*\u0926\u0948\u0928\u093F\u0915 \u0930\u093F\u092A\u094B\u0930\u094D\u091F \u2014 \u092D\u093E\u0917 " + (i + 1) + "/" + total + "*\n" + p;
        });
        window.location.href = "whatsapp://send?text=" + encodeURIComponent(all[0]);
        try {
          window.__shdParts = all;
          toast("\u0930\u093F\u092A\u094B\u0930\u094D\u091F " + total + " \u092D\u093E\u0917\u094B\u0902 \u092E\u0947\u0902 \u0939\u0948 \u2014 \u092A\u0939\u0932\u093E \u092D\u093E\u0917 WhatsApp \u092E\u0947\u0902 \u0916\u0941\u0932\u093E; \u092C\u093E\u0915\u0940 \u0915\u0949\u092A\u0940 \u0938\u0947 \u092D\u0947\u091C\u0947\u0902", "ok", 4200);
        } catch (e) {}
        return;
      }
      window.location.href = "whatsapp://send?text=" + encodeURIComponent(txt);
    },

    copyReport: function () {
      var date = document.getElementById("reportDate").value || S.todayStr();
      var txt = this.buildReportText(date);
      var self = this;
      function done() { toast("\u0930\u093F\u092A\u094B\u0930\u094D\u091F \u0915\u0949\u092A\u0940 \u0939\u094B \u0917\u0908 \u2713", "ok"); }
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(txt).then(done).catch(function () { self._fallbackCopy(txt, done); });
      } else { this._fallbackCopy(txt, done); }
    },

    _fallbackCopy: function (txt, done) {
      try {
        var ta = document.createElement("textarea");
        ta.value = txt; ta.style.position = "fixed"; ta.style.left = "-9999px";
        document.body.appendChild(ta); ta.select();
        document.execCommand("copy");
        document.body.removeChild(ta);
        done();
      } catch (e) { toast("\u0915\u0949\u092A\u0940 \u0928\u0939\u0940\u0902 \u0939\u0941\u0908", "err"); }
    },

    /* ---------- CUSTOMERS ---------- */
    renderCustomers: function () {
      var self = this;
      var list = S.sorted();
      var ms = this.matchSearch();
      var f = this.filter.cust;
      var cont = document.getElementById("customerList");
      cont.innerHTML = "";
      var shown = list.filter(function (c) {
        if (!ms(c)) return false;
        if (f === "active") return num(c.is_active, 1) === 1 && !c.is_archived;
        if (f === "paused") return num(c.is_active, 1) === 0 && !c.is_archived;
        if (f === "archived") return c.is_archived === 1;
        return true;
      });
      if (!list.length) {
        cont.innerHTML = '<div class="empty"><div class="empty-ico">👥</div><h3>No customers yet</h3><p>Add your first customer to start deliveries.</p><button class="btn btn-primary" onclick="App.openCustomer(null)">+ Add Customer</button></div>';
        return;
      }
      if (!shown.length) { cont.innerHTML = '<div class="empty"><div class="empty-ico">🔍</div><h3>Nothing found</h3></div>'; return; }
      shown.forEach(function (c) {
        var sum = Calc.customerSummary(c);
        var card = document.createElement("div");
        card.className = "ccard" + (num(c.is_active, 1) === 0 ? " inactive" : "") + (c.is_archived ? " paused" : "");
        var statusBadge = "";
        if (c.is_archived) statusBadge = '<span class="badge paused">📦 Archived</span>';
        else if (num(c.is_active, 1) === 0) statusBadge = '<span class="badge paused">⏸️ Paused</span>';
        else statusBadge = '<span class="badge monthly">' + sum.days + ' days</span>';

        card.innerHTML = '<div class="ccard-top"><div style="min-width:0"><div class="ccard-name" data-khata="' + c.id + '">' + esc(c.name) + '</div>' +
          '<div class="ccard-meta"><span>📍 ' + esc(c.mohalla || "-") + '</span><span>' + esc(c.milk_type) + '</span><span>' + num(c.rate_per_liter) + ' ₹/L</span><span>' + (c.billing_type === "daily_cash" ? "Daily Cash" : "Monthly") + '</span></div>' +
          '<div class="ccard-meta"><span>' + (c.delivery_frequency === "alternate" ? "🔁 Alternate" : "📅 Daily") + '</span></div></div>' +
          statusBadge + '</div>' +
          '<div class="ccard-meta" style="margin-top:7px"><span>🥛 ' + sum.liters.toFixed(2) + ' L</span><span>Bill ' + money(sum.bill) + '</span>' +
          '<span style="color:' + (sum.due > 0 ? "#DC2626" : "#15803D") + ';font-weight:800">Due ' + money(sum.due) + '</span></div>';
        var act = document.createElement("div");
        act.className = "ccard-actions";
        if (c.is_archived) {
          act.innerHTML =
            '<button class="icon-btn" data-khata="' + c.id + '" title="View Khata">📖</button>' +
            '<button class="btn btn-outline btn-sm" data-unarchive="' + c.id + '" style="margin-left:auto">↩️ Unarchive Customer</button>';
        } else {
          act.innerHTML =
            (c.phone ? '<button class="icon-btn" data-call="' + esc(c.phone) + '" title="Call">📞</button>' : "") +
            '<button class="icon-btn" data-up="' + c.id + '" title="Move Up">⬆️</button>' +
            '<button class="icon-btn" data-down="' + c.id + '" title="Move Down">⬇️</button>' +
            '<button class="icon-btn" data-edit="' + c.id + '" title="Edit">✏️</button>' +
            '<button class="icon-btn" data-toggle="' + c.id + '" title="Toggle Active">' + (num(c.is_active, 1) === 1 ? "⏸️" : "▶️") + '</button>' +
            '<button class="icon-btn" data-del="' + c.id + '" title="Archive Customer" style="color:var(--warning)">📦</button>';
        }
        card.appendChild(act);
        cont.appendChild(card);
      });
    },

    bindCustomerList: function () {
      var self = this;
      document.getElementById("custFilter").addEventListener("click", function (e) {
        var b = e.target.closest(".fseg"); if (!b) return;
        self.filter.cust = b.getAttribute("data-filter");
        document.querySelectorAll("#custFilter .fseg").forEach(function (x) { x.classList.remove("active"); });
        b.classList.add("active"); self.renderCustomers();
      });
      document.getElementById("customerList").addEventListener("click", function (e) {
        var t = e.target.closest("[data-khata],[data-edit],[data-toggle],[data-del],[data-unarchive],[data-up],[data-down],[data-call]");
        if (!t) return;
        if (t.hasAttribute("data-khata")) { self.openKhata(parseInt(t.getAttribute("data-khata"), 10)); return; }
        if (t.hasAttribute("data-call")) { self.call(t.getAttribute("data-call")); return; }
        var id = parseInt(t.getAttribute("data-edit") || t.getAttribute("data-toggle") || t.getAttribute("data-del") || t.getAttribute("data-unarchive") || t.getAttribute("data-up") || t.getAttribute("data-down"), 10);
        var c = S.getCustomer(id); if (!c) return;
        if (t.hasAttribute("data-edit")) { self.openCustomer(c); return; }
        if (t.hasAttribute("data-toggle")) { c.is_active = num(c.is_active, 1) === 1 ? 0 : 1; S.saveDB(S.get()); self.refreshAll(); toast(num(c.is_active, 1) === 1 ? "Milk ON" : "Milk OFF", "ok"); return; }
        if (t.hasAttribute("data-del")) { self.deleteCustomer(c); return; }
        if (t.hasAttribute("data-unarchive")) {
          c.is_archived = 0;
          c.is_active = 1;
          S.saveDB(S.get());
          S.reindexRoute();
          toast(c.name + " unarchived ✓ (Active on route)", "ok");
          self.refreshAll();
          return;
        }
        if (t.hasAttribute("data-up")) { self.move(c, -1); return; }
        if (t.hasAttribute("data-down")) { self.move(c, 1); return; }
      });
    },

    move: function (c, dir) {
      var list = S.sorted();
      var i = list.indexOf(c), j = i + dir;
      if (j < 0 || j >= list.length) return;
      var tmp = list[i]; list[i] = list[j]; list[j] = tmp;
      list.forEach(function (x, k) { x.route_order = k + 1; });
      S.saveDB(S.get()); this.refreshAll();
    },

    deleteCustomer: function (c) {
      var due = Calc.customerBalance(c.id);
      var msg = "⚠️ ARCHIVE CUSTOMER — ZERO DATA LOSS\n\n" +
        "Customer: " + c.name + "\n" +
        (due !== 0 ? "Current Balance Due: " + money(due) + "\n\n" : "\n") +
        "To protect historical business data, permanent deletion is prevented.\n" +
        "Archiving this customer will:\n" +
        "• Remove them from the daily delivery route\n" +
        "• Permanently preserve ALL milk history\n" +
        "• Permanently preserve ALL payments & online records\n" +
        "• Keep Khata ledger and accounting reports 100% accurate\n\n" +
        "Archive " + c.name + "?";
      if (!confirm(msg)) return;
      c.is_active = 0;
      c.is_archived = 1;
      S.saveDB(S.get());
      S.reindexRoute();
      toast(c.name + " archived (Khata safely preserved)", "ok");
      this.refreshAll();
    },

    bindCustomerForm: function () {
      var self = this;
      document.getElementById("addCustomerBtn").addEventListener("click", function () { self.openCustomer(null); });
      document.getElementById("addPortionBtn").addEventListener("click", function () {
        self._builder.push({ id: uid("p"), label: "", liters: 0.5, rate: num(document.getElementById("custRate").value, 0), milk_type: document.getElementById("custMilkType").value });
        self.renderBuilder();
      });
      document.getElementById("saveCustomerBtn").addEventListener("click", function () { self.saveCustomer(); });
    },

    openCustomer: function (c) {
      this._editId = c ? c.id : null;
      document.getElementById("custSheetTitle").textContent = c ? "Edit Customer" : "New Customer";
      document.getElementById("custId").value = c ? c.id : "";
      document.getElementById("custName").value = c ? c.name : "";
      document.getElementById("custPhone").value = c ? (c.phone || "") : "";
      document.getElementById("custMohalla").value = c ? (c.mohalla || "कटरा") : "कटरा";
      document.getElementById("custLandmark").value = c ? (c.landmark || "") : "";
      document.getElementById("custMilkType").value = c ? (c.milk_type || "भैंस") : "भैंस";
      document.getElementById("custLiters").value = c ? num(c.default_liters, 1) : 1;
      document.getElementById("custRate").value = c ? num(c.rate_per_liter, 0) : "";
      document.getElementById("custBilling").value = c ? (c.billing_type || "monthly") : "monthly";
      document.getElementById("custFrequency").value = c ? (c.delivery_frequency || "daily") : "daily";
      var a = c && c.frequency_anchor_date ? c.frequency_anchor_date : S.todayStr();
      document.getElementById("custAnchor").value = String(a).slice(0, 10);
      document.getElementById("custRoute").value = c ? num(c.route_order, "") : "";
      document.getElementById("custActive").value = c && num(c.is_active, 1) === 0 ? "0" : "1";
      document.getElementById("custNote").value = c ? (c.note || "") : "";
      this._builder = c ? S.portionsOf(c).slice() : [];
      this.renderBuilder();
      S.openSheet("customerSheet");
    },

    renderBuilder: function () {
      var self = this;
      var wrap = document.getElementById("portionBuilder");
      wrap.innerHTML = "";
      if (!this._builder.length) { wrap.innerHTML = '<p class="hint">No separate portions - base milk will be used.</p>'; return; }
      this._builder.forEach(function (p, i) {
        var it = document.createElement("div");
        it.className = "pb-item";
        it.innerHTML = '<div class="pb-grid">' +
          '<input class="inp full" placeholder="Label (Lower/Upper/Shop)" value="' + esc(p.label || "") + '" data-f="label" data-i="' + i + '" />' +
          '<input class="inp" type="number" step="0.25" placeholder="Liters" value="' + num(p.liters) + '" data-f="liters" data-i="' + i + '" />' +
          '<input class="inp" type="number" step="1" placeholder="Rate ₹/L" value="' + num(p.rate) + '" data-f="rate" data-i="' + i + '" />' +
          '<select class="inp full" data-f="milk_type" data-i="' + i + '"><option value="भैंस"' + (p.milk_type === "गाय" ? "" : " selected") + '>भैंस</option><option value="गाय"' + (p.milk_type === "गाय" ? " selected" : "") + '>गाय</option></select>' +
          '</div><button class="btn btn-danger btn-xs" data-rm="' + i + '">🗑️ Delete</button>';
        wrap.appendChild(it);
      });
      wrap.querySelectorAll("[data-f]").forEach(function (el) {
        el.addEventListener("change", function () {
          var i = parseInt(el.getAttribute("data-i"), 10), f = el.getAttribute("data-f");
          self._builder[i][f] = (f === "liters" || f === "rate") ? num(el.value) : el.value;
        });
      });
      wrap.querySelectorAll("[data-rm]").forEach(function (b) {
        b.addEventListener("click", function () { self._builder.splice(parseInt(b.getAttribute("data-rm"), 10), 1); self.renderBuilder(); });
      });
    },

    saveCustomer: function () {
      var name = document.getElementById("custName").value.trim();
      var rate = num(document.getElementById("custRate").value, 0);
      if (!name) { toast("Name required is", "err"); return; }
      if (rate <= 0) { toast("Rate ₹/Liters Enter", "err"); return; }
      var db = S.get();
      var portions = this._builder.filter(function (p) { return num(p.liters) > 0; });
      var data = {
        name: name,
        phone: document.getElementById("custPhone").value.trim(),
        mohalla: document.getElementById("custMohalla").value.trim() || "कटरा",
        landmark: document.getElementById("custLandmark").value.trim(),
        note: document.getElementById("custNote").value.trim(),
        milk_type: document.getElementById("custMilkType").value,
        default_liters: num(document.getElementById("custLiters").value, 1),
        rate_per_liter: rate,
        billing_type: document.getElementById("custBilling").value,
        delivery_frequency: document.getElementById("custFrequency").value,
        frequency_anchor_date: document.getElementById("custAnchor").value || S.todayStr(),
        portions_json: JSON.stringify(portions),
        is_active: document.getElementById("custActive").value === "0" ? 0 : 1
      };
      if (this._editId) {
        var c = S.getCustomer(this._editId);
        if (c) Object.keys(data).forEach(function (k) { c[k] = data[k]; });
        var ro = num(document.getElementById("custRoute").value, null);
        if (ro != null) c.route_order = ro;
      } else {
        data.id = S.nextCustomerId();
        data.route_order = num(document.getElementById("custRoute").value, db.customers.length + 1);
        data.created_at = new Date().toISOString();
        db.customers.push(data);
      }
      S.saveDB(db); S.reindexRoute();
      S.closeSheet("customerSheet");
      toast("Customer saved", "ok");
      this.refreshAll();
    },

    /* ---------- KHATA ---------- */
    openKhata: function (cid) {
      var c = S.getCustomer(cid); if (!c) return;
      this._khataId = cid;
      document.getElementById("khataTitle").textContent = c.name;
      var sum = Calc.customerSummary(c);
      document.getElementById("khataHead").innerHTML =
        '<div class="opcard" style="box-shadow:none;border:1.5px solid var(--border)">' +
        '<div class="opcard-name" style="font-size:19px">' + esc(c.name) + '</div>' +
        '<div class="opcard-meta">📍 ' + esc(c.landmark || c.mohalla || "-") + (c.phone ? ' · 📞 ' + esc(c.phone) : "") + '</div>' +
        '<div class="opcard-meta"><span>' + esc(c.milk_type) + '</span><span>' + num(c.rate_per_liter) + ' ₹/L</span><span>' + (c.billing_type === "daily_cash" ? "Daily Cash" : "Monthly") + '</span><span>' + (c.delivery_frequency === "alternate" ? "Alternate" : "Daily") + '</span></div></div>';
      document.getElementById("khataKpis").innerHTML =
        kpi(sum.days, "Total Days") + kpi(sum.liters.toFixed(2) + " L", "Total Milk") +
        kpi(money(sum.bill), "Total Bill") + kpi(money(sum.paid), "Total Paid", "success") +
        kpi(money(sum.cash), "Cash") + kpi(money(sum.online), "UPI") +
        kpi(sum.naaga, "Naaga") + kpi(money(sum.due), "Due", sum.due > 0 ? "danger" : "success");

      var rows = [];
      var db = S.get();
      db.deliveries.filter(function (d) { return d.customer_id === cid; }).forEach(function (d) { rows.push({ t: "d", o: d }); });
      db.payments.filter(function (p) { return p.customer_id === cid; }).forEach(function (p) { rows.push({ t: "p", o: p }); });
      rows.sort(function (a, b) { return (b.o.date + (b.o.delivery_time || b.o.created_at || "")).localeCompare(a.o.date + (a.o.delivery_time || a.o.created_at || "")); });

      var wrap = document.getElementById("khataLedger");
      wrap.innerHTML = "";
      if (!rows.length) { wrap.innerHTML = '<div class="empty" style="padding:28px"><div class="empty-ico">📖</div><h3>No records</h3></div>'; }
      rows.forEach(function (r) {
        var el = document.createElement("div");
        el.className = "ledger-item2" + (r.o.is_void ? " voided" : "");
        if (r.t === "d") {
          var d = r.o;
          var parts = safeParseLocal(d.portions_json, []).map(function (p) { return esc(p.label) + " " + num(p.liters).toFixed(2) + "L"; }).join(", ");
          var paidTxt = [];
          if (Calc.cash(d) > 0) paidTxt.push("Cash " + money(Calc.cash(d)));
          if (Calc.online(d) > 0) paidTxt.push("UPI " + money(Calc.online(d)));
          var titleLabel = parts || esc(d.milk_type);
          if (d.is_adjustment) {
            titleLabel = "⚖️ Extra Charge: " + esc(d.note || "Adjustment");
          }

          var botHtml = "";
          if (d.is_void) {
            botHtml = '<span class="void-tag">⛔ VOIDED</span>' +
              (d.note ? '<span style="font-size:11px;color:var(--text-sub)">📝 ' + esc(d.note) + '</span>' : '') +
              '<button class="btn btn-outline btn-sm" style="padding:2px 8px;font-size:11px" data-unvoidentry="' + d.id + '" title="Restore entry">↩️ Unvoid</button>';
          } else if (d.status !== "delivered") {
            botHtml = '<span class="paid">Naaga</span>' +
              (d.note ? '<span style="font-size:11px;color:var(--text-sub)">📝 ' + esc(d.note) + '</span>' : '') +
              '<button class="row-del" data-voidentry="' + d.id + '" title="Void entry">🚫</button>';
          } else {
            botHtml = '<span class="paid">' + (paidTxt.length ? paidTxt.join(" · ") : "—") + '</span>' +
              (d.note ? '<span style="font-size:11px;color:var(--text-sub)">📝 ' + esc(d.note) + '</span>' : '') +
              '<button class="row-del" data-voidentry="' + d.id + '" title="Void entry">🚫</button>';
          }

          el.innerHTML = '<div class="ledger-top"><span class="ledger-date">' + S.fmtDateShort(d.date) + '</span>' +
            '<span class="ledger-time">' + esc(d.delivery_time || "") + '</span></div>' +
            '<div class="ledger-mid"><span>' + titleLabel + '</span><span><b>' + num(d.liters).toFixed(2) + ' L</b></span><span>' + money(d.total_amount) + '</span></div>' +
            '<div class="ledger-bot">' + botHtml + '</div>';
        } else {
          var p = r.o;
          var payLabel = p.is_adjustment ? ('⚖️ Discount: ' + esc(p.note || "Adjustment")) : ('💵 Payment: ' + esc(p.note || p.payment_mode || ""));
          var pBotHtml = "";
          if (p.is_void) {
            pBotHtml = '<span class="void-tag">⛔ VOIDED</span>' +
              (p.note ? '<span style="font-size:11px;color:var(--text-sub)">📝 ' + esc(p.note) + '</span>' : '') +
              '<button class="btn btn-outline btn-sm" style="padding:2px 8px;font-size:11px" data-unvoidentry="' + p.id + '" title="Restore entry">↩️ Unvoid</button>';
          } else {
            pBotHtml = '<span class="paid" style="color:var(--success);font-weight:800">' + money(p.amount) + '</span>' +
              (p.note ? '<span style="font-size:11px;color:var(--text-sub)">📝 ' + esc(p.note) + '</span>' : '') +
              '<button class="row-del" data-voidentry="' + p.id + '" title="Void entry">🚫</button>';
          }

          el.innerHTML = '<div class="ledger-top"><span class="ledger-date">' + S.fmtDateShort(p.date) + '</span>' +
            '<span class="ledger-time">' + esc(p.payment_subtype || p.payment_mode || "") + '</span></div>' +
            '<div class="ledger-mid"><span>' + payLabel + '</span></div>' +
            '<div class="ledger-bot">' + pBotHtml + '</div>';
        }
        wrap.appendChild(el);
      });
      S.openSheet("khataSheet");
    },

    voidEntry: function (id) {
      if (!confirm("Mark this transaction as VOID?\n\n• It will be excluded from balance calculations\n• Transaction history is NEVER permanently deleted\n• You can unvoid it anytime")) return;
      var db = S.get();
      var found = false;
      db.deliveries.forEach(function (d) {
        if (d.id === id) { d.is_void = true; d.voided_at = new Date().toISOString(); found = true; }
      });
      if (!found) {
        db.payments.forEach(function (p) {
          if (p.id === id) { p.is_void = true; p.voided_at = new Date().toISOString(); found = true; }
        });
      }
      if (!found) return;
      S.saveDB(db);
      this.refreshAll();
      if (this._khataId) this.openKhata(this._khataId);
      toast("Transaction marked VOID (History preserved)", "ok");
    },

    unvoidEntry: function (id) {
      if (!confirm("Restore this transaction back into active balance?")) return;
      var db = S.get();
      var found = false;
      db.deliveries.forEach(function (d) {
        if (d.id === id) { d.is_void = false; delete d.voided_at; found = true; }
      });
      if (!found) {
        db.payments.forEach(function (p) {
          if (p.id === id) { p.is_void = false; delete p.voided_at; found = true; }
        });
      }
      if (!found) return;
      S.saveDB(db);
      this.refreshAll();
      if (this._khataId) this.openKhata(this._khataId);
      toast("Transaction restored to active ✓", "ok");
    },

    deleteEntry: function (id) {
      // Safe redirect: permanently deleting business transactions is disabled for data protection
      this.voidEntry(id);
    },

    openAdj: function () {
      var c = S.getCustomer(this._khataId); if (!c) return;
      document.getElementById("adjTitle").textContent = "Khata Adjustment — " + c.name;
      document.getElementById("adjAmount").value = "";
      document.getElementById("adjNote").value = "";
      document.getElementById("adjType").value = "discount";
      S.openSheet("adjSheet");
    },

    saveAdj: function () {
      var c = S.getCustomer(this._khataId); if (!c) return;
      var type = document.getElementById("adjType").value;
      var amt = num(document.getElementById("adjAmount").value, 0);
      var note = document.getElementById("adjNote").value.trim();
      if (amt <= 0) { toast("Valid amount required", "err"); return; }
      var db = S.get();
      var today = S.todayStr();
      if (type === "discount") {
        db.payments.push({
          id: uid("adj"),
          customer_id: c.id,
          date: today,
          amount: r2(amt),
          payment_mode: "Discount",
          payment_subtype: "Adjustment",
          note: note || "छूट / बट्टा (Discount)",
          is_adjustment: true,
          adjustment_type: "discount",
          is_void: false,
          created_at: new Date().toISOString()
        });
      } else {
        db.deliveries.push({
          id: uid("adj"),
          customer_id: c.id,
          date: today,
          delivery_time: S.timeStr(),
          milk_type: "शुल्क (Charge)",
          liters: 0,
          rate: 0,
          total_amount: r2(amt),
          paid_cash: 0,
          paid_online: 0,
          total_paid: 0,
          status: "delivered",
          portions_json: "[]",
          is_adjustment: true,
          adjustment_type: "charge",
          note: note || "अतिरिक्त शुल्क (Extra Charge)",
          is_void: false,
          created_at: new Date().toISOString()
        });
      }
      S.saveDB(db);
      S.closeSheet("adjSheet");
      toast("Adjustment saved ✓", "ok");
      this.refreshAll();
      this.openKhata(c.id);
    },

    openPayment: function (mode) {
      var c = S.getCustomer(this._khataId); if (!c) return;
      var due = Calc.customerBalance(c.id);
      document.getElementById("paymentTitle").textContent = (mode === "Cash" ? "Cash" : "Online / UPI") + " Payment — " + c.name;
      document.getElementById("payAmount").value = due > 0 ? r0(due) : "";
      document.getElementById("payMode").value = mode === "Cash" ? "Cash" : "UPI";
      document.getElementById("payNote").value = mode === "Cash" ? "Cash Payment" : "UPI Payment";
      S.openSheet("paymentSheet");
    },

    savePayment: function () {
      var c = S.getCustomer(this._khataId); if (!c) return;
      var amt = num(document.getElementById("payAmount").value, 0);
      if (amt <= 0) { toast("Amount Enter", "err"); return; }
      var mode = document.getElementById("payMode").value;
      var db = S.get();
      db.payments.push({
        id: uid("pay"), customer_id: c.id, date: S.todayStr(), amount: r2(amt),
        payment_mode: mode === "Cash" ? "Cash" : "UPI",
        payment_subtype: mode,
        note: document.getElementById("payNote").value.trim() || "Payment",
        created_at: new Date().toISOString()
      });
      S.saveDB(db);
      S.closeSheet("paymentSheet");
      S.speak(c.name + ", " + r0(amt) + " Rupees received.");
      toast(money(amt) + " Payment", "ok");
      this.refreshAll();
      this.openKhata(c.id);
    },

    waBill: function () {
      var c = S.getCustomer(this._khataId); if (!c) return;
      var sum = Calc.customerSummary(c);
      var st = S.settings();
      var upi = "";
      if (sum.due > 0 && st.upi) {
        upi = "upi://pay?pa=" + encodeURIComponent(st.upi) + "&pn=" + encodeURIComponent(st.payee || "Shahbuddin Dairy") +
          "&am=" + sum.due.toFixed(2) + "&cu=INR&tn=" + encodeURIComponent("Milk Bill Shahbuddin Dairy");
      }
      var msg = "*Shahbuddin Dairy*\nGulab Nagar, Bareilly\n\nHello " + c.name + " ji,\n\n" +
        "Total Milk: " + sum.liters.toFixed(2) + " L\n" +
        "Total Bill: " + money(sum.bill) + "\n" +
        "Cash Payment: " + money(sum.cash) + "\n" +
        "Online Payment: " + money(sum.online) + "\n\n" +
        "*Total Due: " + money(sum.due) + "*\n\n" +
        (upi ? "UPI payment: " + upi + "\n\n" : "") +
        "Thank you\nShahbuddin Dairy";
      var ph = (c.phone || "").replace(/[^0-9]/g, "");
      window.location.href = ph ? "whatsapp://send?phone=91" + ph + "&text=" + encodeURIComponent(msg) : "whatsapp://send?text=" + encodeURIComponent(msg);
    },

    upiPay: function () {
      var c = S.getCustomer(this._khataId); if (!c) return;
      var st = S.settings();
      if (!st.upi) { toast("first Settings in UPI ID Enter", "err"); return; }
      var due = Calc.customerBalance(c.id);
      if (due <= 0) { toast("No Due No", "ok"); return; }
      var url = "upi://pay?pa=" + encodeURIComponent(st.upi) + "&pn=" + encodeURIComponent(st.payee || "Shahbuddin Dairy") +
        "&am=" + due.toFixed(2) + "&cu=INR&tn=" + encodeURIComponent("Milk Bill Shahbuddin Dairy");
      window.location.href = url;
    },

    /* ---------- ACCOUNTS ---------- */
    renderAccounts: function () {
      var db = S.get();
      var totBill = 0, totPaid = 0, totDue = 0, totL = 0, cash = 0, online = 0;
      db.customers.forEach(function (c) {
        var s = Calc.customerSummary(c);
        totBill += s.bill; totPaid += s.paid; totDue += s.due; totL += s.liters;
        cash += s.cash; online += s.online;
      });
      var deliv = db.deliveries.filter(function (d) { return d.status === "delivered"; }).length;
      document.getElementById("accountsKpis").innerHTML =
        kpi(money(Calc.marketDebt()), "Total Market Due", totDue > 0 ? "danger" : "success") +
        kpi(db.customers.length, "Total Customers") +
        kpi(deliv, "Total Deliveries") +
        kpi(totL.toFixed(2) + " L", "Total Milk") +
        kpi(money(totBill), "Total Bill") +
        kpi(money(cash), "Total Cash", "success") +
        kpi(money(online), "Total UPI", "success") +
        kpi(money(totPaid), "Total Paid", "success");

      var today = S.todayStr();
      var proc = db.procurement.filter(function (p) { return p.date === today; })[0] || {};
      var dayL = Calc.dailySummary(today).soldL;
      var arrived = num(proc.procured_liters, 0);
      document.getElementById("stockKpis").innerHTML =
        kpi(arrived.toFixed(1) + " L", "Today In") +
        kpi(dayL.toFixed(1) + " L", "Today Sold") +
        kpi(arrived > 0 ? (arrived - dayL).toFixed(1) + " L" : "—", "Pending Stock");
      if (window.BackupSystem && BackupSystem.updateStatus) BackupSystem.updateStatus();
    },

    bindAccounts: function () {
      var self = this;
      document.getElementById("accountsExcelBtn").addEventListener("click", function () { self.exportExcel(); });
      var bBtn = document.getElementById("backupBtn");
      if (bBtn) bBtn.addEventListener("click", function () { BackupSystem.exportBackup(); });
      var rFile = document.getElementById("restoreFile");
      if (rFile) rFile.addEventListener("change", function (e) { BackupSystem.onRestoreFileInput(e); });
      var vFile = document.getElementById("verifyBackupFile");
      if (vFile) vFile.addEventListener("change", function (e) { BackupSystem.onVerifyFileInput(e); });
      var sBtn = document.getElementById("restoreSafetyBackupBtn");
      if (sBtn) sBtn.addEventListener("click", function () { BackupSystem.revertSafetyBackup(); });
      document.getElementById("saveProcBtn").addEventListener("click", function () { self.saveProc(); });
      document.getElementById("saveSettingsBtn").addEventListener("click", function () {
        var st = S.settings();
        st.upi = document.getElementById("setUpi").value.trim();
        st.payee = document.getElementById("setPayee").value.trim() || "Shahbuddin Dairy";
        st.phone = document.getElementById("setPhone").value.trim();
        S.setSettings(st);
        toast("Settings saved", "ok");
      });
      var wipeBtn = document.getElementById("wipeBtn");
      if (wipeBtn) {
        wipeBtn.addEventListener("click", function () {
          toast("Database reset is permanently disabled to guarantee Zero Data Loss", "err");
        });
      }
    },

    saveProc: function () {
      var db = S.get();
      var today = S.todayStr();
      db.procurement = db.procurement.filter(function (p) { return p.date !== today; });
      db.procurement.push({
        date: today,
        procured_liters: num(document.getElementById("procLiters").value, 0),
        cost_price_per_liter: num(document.getElementById("procRate").value, 0),
        supplier: document.getElementById("procSupplier").value.trim()
      });
      S.saveDB(db);
      toast("Today's purchase saved", "ok");
      this.renderAccounts();
    },

    exportExcel: function () {
      if (typeof XLSX === "undefined") { toast("Excel engine load No done", "err"); return; }
      var db = S.get();
      var S1 = [["Customer ID", "Name", "Mobile", "Area", "Landmark", "Milk Type", "Default L", "Rate (₹/L)", "Account", "Schedule", "Total Milk (L)", "Total Bill (₹)", "Cash (₹)", "UPI (₹)", "Due (₹)", "Status"]];
      S.sorted().forEach(function (c) {
        var s = Calc.customerSummary(c);
        S1.push([c.id, c.name, c.phone || "", c.mohalla || "", c.landmark || "", c.milk_type || "", num(c.default_liters, 1), num(c.rate_per_liter),
          c.billing_type === "daily_cash" ? "Daily Cash" : "Monthly",
          c.delivery_frequency === "alternate" ? "Alternate" : "Daily",
          r2(s.liters), r2(s.bill), r2(s.cash), r2(s.online), r2(s.due), num(c.is_active, 1) === 1 ? "Active" : "Paused"]);
      });
      var S2 = [["Log ID","Date","Time","Customer","Area","Portions","Total L","Rate","Total Bill","Cash","UPI","Status"]];
      db.deliveries.slice().sort(function (a, b) { return (b.date).localeCompare(a.date); }).forEach(function (d) {
        var c = S.getCustomer(d.customer_id) || {};
        var parts = safeParseLocal(d.portions_json, []).map(function (p) { return p.label + " " + num(p.liters).toFixed(2) + "L@" + num(p.rate); }).join(" | ");
        S2.push([d.id, S.fmtDate(d.date), d.delivery_time || "", c.name || "", c.mohalla || "", parts,
          num(d.liters), num(d.rate), num(d.total_amount), Calc.cash(d), Calc.online(d),
          d.status === "delivered" ? "Delivered" : "Naaga"]);
      });
      var S3 = [["Receipt ID","Date","Customer","Amount (₹)","Mode","Note","Timestamp"]];
      db.payments.slice().sort(function (a, b) { return (b.date).localeCompare(a.date); }).forEach(function (p) {
        var c = S.getCustomer(p.customer_id) || {};
        S3.push([p.id, S.fmtDate(p.date), c.name || "", num(p.amount), p.payment_subtype || p.payment_mode || "Cash", p.note || "", p.created_at || ""]);
      });
      var wb = XLSX.utils.book_new();
      var ws1 = XLSX.utils.aoa_to_sheet(S1), ws2 = XLSX.utils.aoa_to_sheet(S2), ws3 = XLSX.utils.aoa_to_sheet(S3);
      styleSheet(ws1, S1); styleSheet(ws2, S2); styleSheet(ws3, S3);
      XLSX.utils.book_append_sheet(wb, ws1, "Customer_Summary");
      XLSX.utils.book_append_sheet(wb, ws2, "Daily_Milk_Sales");
      XLSX.utils.book_append_sheet(wb, ws3, "Payment_Receipts");
      var out = XLSX.write(wb, { bookType: "xlsx", type: "base64" });
      S.downloadUri("data:application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;base64," + out, "ShahbuddinDairy_" + S.todayStr().replace(/-/g, "_") + ".xlsx");
      toast("Excel created — Downloads in Save will", "ok");

      function styleSheet(ws, aoa) {
        if (!ws["!ref"]) return;
        var cols = aoa[0].length;
        ws["!cols"] = [];
        for (var cI = 0; cI < cols; cI++) {
          var w = 10;
          for (var rI = 0; rI < Math.min(aoa.length, 80); rI++) { var v = aoa[rI][cI]; if (v != null) w = Math.max(w, String(v).length + 3); }
          ws["!cols"].push({ wch: Math.min(w, 42) });
        }
        for (var ci = 0; ci < cols; ci++) {
          var a = XLSX.utils.encode_cell({ r: 0, c: ci });
          if (!ws[a]) continue;
          ws[a].s = { fill: { fgColor: { rgb: "004D40" } }, font: { bold: true, color: { rgb: "FFFFFF" } }, alignment: { horizontal: "center", vertical: "center", wrapText: true } };
        }
      }
    },

    backup: function () {
      BackupSystem.exportBackup();
    },

    restore: function (ev) {
      BackupSystem.onRestoreFileInput(ev);
    },

    /* ---------- ADHOC PORTION ---------- */
    openAdhoc: function (cid) {
      var c = S.getCustomer(cid); if (!c) return;
      document.getElementById("apLabel").value = "";
      document.getElementById("apLiters").value = "0.5";
      document.getElementById("apRate").value = num(c.rate_per_liter);
      document.getElementById("apMilkType").value = c.milk_type || "भैंस";
      document.getElementById("saveAdhocBtn")._cid = cid;
      S.openSheet("portionSheet");
    },
    saveAdhoc: function () {
      var cid = document.getElementById("saveAdhocBtn")._cid;
      var c = S.getCustomer(cid); if (!c) return;
      var p = {
        id: uid("p"),
        label: document.getElementById("apLabel").value.trim() || "Custom",
        liters: num(document.getElementById("apLiters").value, 0.5),
        rate: num(document.getElementById("apRate").value, num(c.rate_per_liter)),
        milk_type: document.getElementById("apMilkType").value
      };
      if (p.liters <= 0) { toast("Liters Enter", "err"); return; }
      var date = S.todayStr();
      if (S.deliveryFor(c.id, date)) { toast("Entry already exists today - remove it first", "err"); S.closeSheet("portionSheet"); return; }
      var db = S.get();
      db.deliveries.push({
        id: uid("d"), customer_id: c.id, date: date, delivery_time: S.timeStr(),
        milk_type: p.milk_type, liters: p.liters, rate: p.rate,
        total_amount: r2(p.liters * p.rate), paid_cash: 0, paid_online: 0, total_paid: 0,
        status: "delivered", portions_json: JSON.stringify([p])
      });
      S.saveDB(db); S.closeSheet("portionSheet");
      S.speak(c.name + ", " + p.liters.toFixed(2) + " Liters milk delivered.");
      toast("Custom portion added", "ok");
      this.refreshAll();
    }
  };

  /* ================= ROUTE RUNNER ================= */
  var Runner = {
    idx: 0, date: "", state: [], cash: 0, online: 0, oldDue: 0, cur: null,
    autoCalc: false, _saving: false, _suggestedQty: 0,

    init: function () {
      this.date = S.todayStr();
      this.autoCalc = localStorage.getItem("shd_auto_calc") === "1";
      this.bind();
      this.bindSaveConfirm();
      this.refresh();
    },

    refresh: function () {
      if (!this.date) this.date = S.todayStr();
      var list = S.routeList(this.date);
      if (this.idx >= list.length) this.idx = Math.max(0, list.length - 1);
      this.render();
    },

    bindSaveConfirm: function () {
      var self = this;
      var btn = document.getElementById("confirmSaveBtn");
      if (!btn || btn._bound) return;
      btn._bound = true;
      btn.addEventListener("click", function () {
        if (self._saving) return;
        self._saving = true;
        btn.disabled = true;

        var c = self.cur;
        if (!c) { self._saving = false; btn.disabled = false; return; }

        var portions = self.state.filter(function (p) { return num(p.liters) > 0; });
        if (!portions.length) {
          toast("दूध की मात्रा कम से कम 0.05 L दर्ज करें", "err");
          self._saving = false;
          btn.disabled = false;
          return;
        }

        var bill = Calc.deliveryBill(portions);
        var l = Calc.deliveryLiters(portions);
        var date = self.date;
        var note = (document.getElementById("saveNoteInput").value || "").trim();

        var db = S.get();
        var existing = S.deliveryFor(c.id, date);
        var recId = (existing && existing.id) ? existing.id : uid("d");
        var finalOnline = (self.online > 0) ? r2(self.online) : (existing ? num(existing.paid_online, 0) : 0);
        var recCreated = (existing && existing.created_at) ? existing.created_at : new Date().toISOString();

        var foundIdx = -1;
        for (var di = 0; di < db.deliveries.length; di++) {
          if (db.deliveries[di].customer_id === c.id && db.deliveries[di].date === date) {
            foundIdx = di;
            break;
          }
        }
        var rec = {
          id: recId,
          customer_id: c.id,
          date: date,
          delivery_time: S.timeStr(),
          milk_type: portions[0].milk_type || c.milk_type,
          liters: l,
          rate: l > 0 ? r2(bill / l) : num(c.rate_per_liter),
          total_amount: bill,
          paid_cash: r2(self.cash),
          paid_online: finalOnline,
          total_paid: r2(num(self.cash) + finalOnline),
          status: "delivered",
          portions_json: JSON.stringify(portions),
          note: note,
          is_void: false,
          created_at: recCreated
        };
        if (foundIdx >= 0) {
          db.deliveries[foundIdx] = rec;
        } else {
          db.deliveries.push(rec);
        }
        S.saveDB(db);
        S.closeSheet("saveConfirmSheet");
        self._saving = false;
        btn.disabled = false;

        S.speak(c.name + ", " + l.toFixed(2) + " Liters milk delivered.");
        var fb = Calc.financialBreakdown(c.id, date, portions, self.cash, finalOnline);
        App.showSuccess("✓ " + c.name, l.toFixed(2) + " L • बिल " + money(bill) + " • जमा " + money(rec.total_paid) + (fb.remainingDue > 0 ? " • बकाया " + money(fb.remainingDue) : " • बकाया ₹0"), function () {
          var d2 = S.get();
          d2.deliveries.forEach(function (x) {
            if (x.id === rec.id) {
              x.is_void = true;
              x.voided_at = new Date().toISOString();
              x.note = (x.note ? x.note + " (Undone)" : "Undone entry");
            }
          });
          S.saveDB(d2);
          toast("Entry undone (marked void)", "ok");
          App.refreshAll();
        });
        App.refreshAll();
      });
    },

    bind: function () {
      var self = this;
      var el = document.getElementById("taskRunnerContainer");
      el.addEventListener("click", function (e) {
        var t = e.target.closest("[data-a]");
        if (!t) return;
        var a = t.getAttribute("data-a");
        if (a === "prev") self.prev();
        else if (a === "next") self.next();
        else if (a === "drawer") App.openRouteDrawer();
        else if (a === "step") self.step(parseInt(t.getAttribute("data-i"), 10), parseFloat(t.getAttribute("data-d")));
        else if (a === "chip") self.chip(parseInt(t.getAttribute("data-i"), 10), parseFloat(t.getAttribute("data-amt")));
        else if (a === "adhoc") App.openAdhoc(self.cur.id);
        else if (a === "commit") self.commit();
        else if (a === "naaga") self.naaga();
        else if (a === "later") self.next();
        else if (a === "call") App.call(self.cur.phone);
        else if (a === "wa") self.wa();
        else if (a === "khata") App.openKhata(self.cur.id);
        else if (a === "serveoff") self.commit();
        else if (a === "delportion") { self.state.splice(parseInt(t.getAttribute("data-i"), 10), 1); self.redraw(); }
        else if (a === "filter") self.setFilter(t.getAttribute("data-f"));
        else if (a === "setDate") {
          self.date = t.getAttribute("data-val") || S.todayStr();
          self.refresh();
        }
      });

      el.addEventListener("input", function (e) {
        var t = e.target;
        if (t.id === "mainMilkInput") {
          var val = Math.max(0, r2(num(t.value)));
          if (self.state[0]) self.state[0].liters = val;
          self.drawPortions();
          self.updateMaths();
        } else if (t.hasAttribute("data-rate")) {
          self.state[parseInt(t.getAttribute("data-rate"), 10)].rate = num(t.value);
          self.updateMaths();
        } else if (t.hasAttribute("data-lit")) {
          var i = parseInt(t.getAttribute("data-lit"), 10);
          self.state[i].liters = Math.max(0, r2(num(t.value)));
          self.updateMaths();
        } else if (t.id === "runCash") {
          self.cash = Math.max(0, num(t.value));
          self.updateMaths();
        }
      });

      el.addEventListener("change", function (e) {
        var t = e.target;
        if (t.id === "runnerDatePicker") {
          self.date = t.value || S.todayStr();
          self.refresh();
        } else if (t.id === "autoCalcToggle") {
          self.autoCalc = t.checked;
          localStorage.setItem("shd_auto_calc", self.autoCalc ? "1" : "0");
          self.updateMaths();
        }
      });

      el.addEventListener("click", function (e) {
        if (e.target && (e.target.id === "useSuggestedBtn" || e.target.closest("#useSuggestedBtn"))) {
          if (self._suggestedQty > 0) {
            if (self.state[0]) self.state[0].liters = self._suggestedQty;
            var mi = document.getElementById("mainMilkInput");
            if (mi) mi.value = self._suggestedQty.toFixed(2);
            self.drawPortions();
            self.updateMaths();
            toast("Suggested Milk Applied: " + self._suggestedQty.toFixed(2) + " L", "ok");
          }
        }
      });

      this.bindGestures();
    },

    bindGestures: function () {
      var self = this;
      if (document._shdGestures) return;
      document._shdGestures = true;
      var sx = 0, sy = 0, st = 0, tracking = false;
      function inDailyTask() {
        var v = document.getElementById("view-task");
        return v && v.classList.contains("active");
      }
      function isFormField(t) {
        if (!t || !t.tagName) return false;
        var n = t.tagName.toLowerCase();
        return n === "input" || n === "textarea" || n === "select" || (t.isContentEditable === true);
      }
      document.addEventListener("touchstart", function (e) {
        if (!inDailyTask()) return;
        if (isFormField(e.target)) { tracking = false; return; }
        sx = e.touches[0].clientX; sy = e.touches[0].clientY;
        st = Date.now(); tracking = true;
      }, { passive: true, capture: true });
      document.addEventListener("touchend", function (e) {
        if (!tracking) return;
        tracking = false;
        if (!inDailyTask()) return;
        var dt = Date.now() - st;
        var dx = sx - e.changedTouches[0].clientX;
        var dy = sy - e.changedTouches[0].clientY;
        var adx = Math.abs(dx), ady = Math.abs(dy);
        if (dt > 900) return;
        if (adx < 30 && ady < 30) return;
        if (adx > ady * 1.5 && adx > 70) {
          if (dx > 0) self.next(); else self.prev();
          return;
        }
        if (ady > adx * 1.5 && dy < -70 && ady > 100) {
          window.scrollTo({ top: 0, behavior: "smooth" });
        }
      }, { passive: true, capture: true });
    },

    filter: "all",
    setFilter: function (f) {
      this.filter = f;
      var date = this.date;
      var list = S.routeList(date);
      if (f !== "all") {
        for (var i = 0; i < list.length; i++) {
          var c = list[i], d = S.deliveryFor(c.id, date);
          var ok = false;
          if (f === "pending") ok = !d;
          else if (f === "done") ok = d && d.status === "delivered";
          else if (f === "naaga") ok = d && d.status === "skipped";
          else if (f === "alt") ok = c.delivery_frequency === "alternate";
          if (ok) { this.idx = i; break; }
        }
      }
      this.render();
    },

    render: function () {
      var cont = document.getElementById("taskRunnerContainer");
      var date = this.date;
      var list = S.routeList(date);
      var yesterday = prevDayStr(S.todayStr());

      var dateBar = '<div class="runner-date-bar">' +
        '<span>📅 <b>दिनांक:</b> ' + S.fmtDateShort(date) + '</span>' +
        '<div class="runner-date-pills">' +
          '<button class="runner-date-pill ' + (date === S.todayStr() ? "active" : "") + '" data-a="setDate" data-val="' + S.todayStr() + '">Today</button>' +
          '<button class="runner-date-pill ' + (date === yesterday ? "active" : "") + '" data-a="setDate" data-val="' + yesterday + '">Yesterday</button>' +
          '<input type="date" id="runnerDatePicker" class="runner-date-inp" value="' + date + '" />' +
        '</div>' +
      '</div>';

      if (!list.length) {
        var total = S.sorted().filter(function (c) { return num(c.is_active, 1) === 1 && !c.is_archived; }).length;
        if (!total) {
          cont.innerHTML = dateBar + '<div class="empty"><div class="empty-ico">🐄</div><h3>Route ready</h3><p>No customers yet।<br>Add a customer first.</p><button class="btn btn-primary" onclick="App.openCustomer(null)">+ Add Customer</button></div>';
        } else {
          cont.innerHTML = dateBar + '<div class="empty"><div class="empty-ico">🌙</div><h3>All customers are off on this date</h3><p>Alternate-day customers are off on ' + S.fmtDate(date) + '.</p></div>';
        }
        return;
      }
      if (this.idx < 0 || this.idx >= list.length) this.idx = 0;
      var c = list[this.idx];
      this.cur = c;
      var d = S.deliveryFor(c.id, date);
      this.oldDue = Calc.prevDueBefore(c.id, date);
      this.state = (d && d.status === "delivered" && d.portions_json)
        ? safeParseLocal(d.portions_json, S.effectivePortions(c))
        : S.effectivePortions(c);
      this.state = this.state.map(function (p) {
        return { id: p.id || uid("p"), label: p.label || "Milk", liters: num(p.liters), rate: num(p.rate, num(c.rate_per_liter)), milk_type: p.milk_type || c.milk_type };
      });
      this.cash = d ? Calc.cash(d) : 0;
      this.online = d ? Calc.online(d) : 0; // preserved in memory

      var prog = Calc.routeProgress();
      var status = !d ? "" : (d.status === "delivered" ? " delivered" : " skipped");
      var badges = '<span class="badge ' + (c.billing_type === "daily_cash" ? "daily_cash" : "monthly") + '">' + (c.billing_type === "daily_cash" ? "Daily Cash" : "Monthly") + '</span>' +
        '<span class="badge ' + (c.delivery_frequency === "alternate" ? "alt" : "monthly") + '">' + (c.delivery_frequency === "alternate" ? "Alternate" : "Daily") + '</span>';
      if (d && d.status === "delivered") badges += '<span class="badge done">✓ ' + esc(d.delivery_time || "") + '</span>';
      if (d && d.status === "skipped") badges += '<span class="badge paused">Naaga</span>';

      var pos = num(c.route_order, this.idx + 1);
      var currentLit = Calc.deliveryLiters(this.state);

      var html = '<div class="runner-wrap">' +
        dateBar +
        '<div class="runner-top"><span class="runner-title">🚚 Daily Task</span>' +
        '<span class="runner-count">' + String(this.idx + 1).padStart(2, "0") + ' / ' + String(list.length).padStart(2, "0") + '</span></div>' +
        '<div class="progress-track"><div class="progress-fill" style="width:' + prog.pct + '%"></div></div>' +
        '<div class="filter-row">' +
          '<button class="fseg ' + (this.filter === "all" ? "active" : "") + '" data-a="filter" data-f="all">All</button>' +
          '<button class="fseg ' + (this.filter === "pending" ? "active" : "") + '" data-a="filter" data-f="pending">Pending</button>' +
          '<button class="fseg ' + (this.filter === "done" ? "active" : "") + '" data-a="filter" data-f="done">Done</button>' +
          '<button class="fseg ' + (this.filter === "naaga" ? "active" : "") + '" data-a="filter" data-f="naaga">Naaga</button>' +
          '<button class="fseg ' + (this.filter === "alt" ? "active" : "") + '" data-a="filter" data-f="alt">Alternate</button>' +
          '<button class="fseg" data-a="drawer">☰ List</button>' +
        '</div>' +
        '<div class="hero' + status + '">' +
          '<div class="hero-top"><div style="display:flex;gap:10px;min-width:0">' +
            '<span class="hero-pos">' + (pos < 10 ? "0" + pos : pos) + '</span>' +
            '<div style="min-width:0"><div class="hero-name" data-a="khata">' + esc(c.name) + '</div>' +
            '<div class="hero-meta"><span>📍 ' + esc(c.landmark || c.mohalla || "-") + '</span>' +
            (c.phone ? '<a href="javascript:void(0)" data-a="call">📞 ' + esc(c.phone) + '</a>' : "") + '</div></div></div>' +
            '<div class="hero-badges">' + badges + '</div></div>' +
          '<div class="sec"><div class="sec-title">Today\'s Milk (Manual Keyboard Entry)</div>' +
            '<div class="milk-main"><div class="milk-ctl">' +
              '<button class="milk-btn" data-a="step" data-i="0" data-d="-0.25">−</button>' +
              '<div class="milk-val-box">' +
                '<input type="number" step="0.05" min="0" class="milk-val-inp" id="mainMilkInput" value="' + currentLit.toFixed(2) + '" />' +
                '<span class="milk-val-unit">LITER</span>' +
              '</div>' +
              '<button class="milk-btn" data-a="step" data-i="0" data-d="0.25">+</button></div>' +
              '<div class="qchips">' +
                '<button class="qchip" data-a="chip" data-i="0" data-amt="-0.2">-200g</button>' +
                '<button class="qchip" data-a="chip" data-i="0" data-amt="0.2">+200g</button>' +
                '<button class="qchip" data-a="chip" data-i="0" data-amt="-0.5">-500g</button>' +
                '<button class="qchip" data-a="chip" data-i="0" data-amt="0.5">+500g</button>' +
                '<button class="qchip" data-a="chip" data-i="0" data-amt="1">+1L</button>' +
              '</div>' +
              '<div class="auto-calc-box">' +
                '<label class="auto-calc-lbl">' +
                  '<input type="checkbox" id="autoCalcToggle"' + (this.autoCalc ? ' checked' : '') + '> ' +
                  '<span>AUTO CALCULATION ON</span>' +
                '</label>' +
                '<div class="suggested-banner" id="suggestedQtyBanner" style="display:none;">' +
                  '<div class="sugg-text">Suggested Milk: <b id="suggestedQtyVal">0.00 L</b></div>' +
                  '<button class="btn btn-outline btn-sm" id="useSuggestedBtn" type="button">USE SUGGESTED QUANTITY</button>' +
                '</div>' +
              '</div>' +
            '</div></div>' +
          (this.state.length > 1 ? '<div class="sec"><div class="sec-title">Portions (' + this.state.length + ')</div><div id="runPortions"></div><button class="btn btn-outline btn-sm" data-a="adhoc" style="width:100%;margin-top:4px">+ New Portion</button></div>' : '<div id="runPortions" style="display:none"></div>') +
          '<div class="sec"><div class="sec-title">Summary &amp; Payment</div>' +
            '<div class="finpanel">' +
              '<div class="finrow bill"><span class="fl">Today\'s Bill</span><span class="fv" id="billAmt">₹0</span></div>' +
              '<div class="finrow old ' + (this.oldDue > 0 ? "" : "zero") + '"><span class="fl">Previous Due</span><span class="fv" id="prevDueAmt">' + money(this.oldDue) + '</span></div>' +
              '<div class="finrow req"><span class="fl">Total Required</span><span class="fv" id="totalReqAmt">₹0</span></div>' +
              '<div class="finpay">' +
                '<div class="pay-field cash" style="width:100%"><label>Cash ₹ (Today\'s Collection)</label><input type="number" id="runCash" min="0" value="' + (this.cash || "") + '" placeholder="0" /></div>' +
              '</div>' +
              (this.online > 0 ? '<div class="finrow" style="font-size:12px;color:var(--muted)"><span>Previous Online: ' + money(this.online) + '</span></div>' : '') +
              '<div class="finrow"><span class="fl">Today\'s Payment</span><span class="fv" id="paidAmt">₹0</span></div>' +
              '<div class="finrow net" id="netRow"><span class="fl">Remaining Due</span><span class="fv" id="netAmt">₹0</span></div>' +
              '<div class="finrow adv" id="advRow" style="display:none"><span class="fl">Advance / Extra</span><span class="fv" id="advAmt">₹0</span></div>' +
            '</div></div>' +
          '<button class="act-primary" data-a="commit">✓ दूध दिया</button>' +
          '<div class="act-row">' +
            '<button class="act-btn" data-a="prev">◀</button>' +
            '<button class="act-btn naaga" data-a="naaga">नागा</button>' +
            '<button class="act-btn" data-a="later">बाद में</button>' +
            '<button class="act-btn" data-a="next">▶</button>' +
            (c.phone ? '<button class="act-btn" data-a="call">📞</button>' : "") +
            '<button class="act-btn wa" data-a="wa">💬</button>' +
          '</div>' +
        '</div>' +
      '</div>';
      cont.innerHTML = html;
      this.drawPortions();
      this.updateMaths();
    },

    drawPortions: function () {
      var self = this;
      var wrap = document.getElementById("runPortions");
      if (!wrap) return;
      wrap.innerHTML = "";
      this.state.forEach(function (p, i) {
        var row = document.createElement("div");
        row.className = "portion-row";
        row.innerHTML = '<span class="portion-label">' + esc(p.label) + '</span>' +
          '<div style="display:flex;align-items:center;gap:5px">' +
            '<button class="mini-btn" data-a="step" data-i="' + i + '" data-d="-0.25">−</button>' +
            '<input class="portion-lit" style="width:74px;border:1.5px solid var(--border);border-radius:10px;padding:6px 4px" type="number" step="0.25" min="0" value="' + num(p.liters).toFixed(2) + '" data-lit="' + i + '" />' +
            '<button class="mini-btn" data-a="step" data-i="' + i + '" data-d="0.25">+</button>' +
          '</div>' +
          '<div class="rate-input">₹<input type="number" step="1" min="0" value="' + num(p.rate) + '" data-rate="' + i + '" /></div>' +
          '<span class="portion-amt">' + money(Calc.portionTotal(p)) + '</span>' +
          (self.state.length > 1 ? '<button class="row-del" data-a="delportion" data-i="' + i + '">🗑️</button>' : "");
        wrap.appendChild(row);
      });
    },

    redraw: function () {
      if (!this.state.length) this.state = [{ id: uid("p"), label: "Milk", liters: 1, rate: num(this.cur.rate_per_liter), milk_type: this.cur.milk_type }];
      this.drawPortions();
      this.updateMaths();
    },

    step: function (i, d) {
      if (!this.state[i]) return;
      this.state[i].liters = Math.max(0, r2(num(this.state[i].liters) + d));
      if (i === 0) {
        var mainInp = document.getElementById("mainMilkInput");
        if (mainInp) mainInp.value = this.state[0].liters.toFixed(2);
      }
      var inp = document.querySelector('[data-lit="' + i + '"]');
      if (inp) inp.value = this.state[i].liters.toFixed(2);
      this.drawPortions();
      this.updateMaths();
    },

    chip: function (i, amt) {
      if (!this.state[i]) return;
      this.state[i].liters = Math.max(0, r2(num(this.state[i].liters) + amt));
      if (i === 0) {
        var mainInp = document.getElementById("mainMilkInput");
        if (mainInp) mainInp.value = this.state[0].liters.toFixed(2);
      }
      this.drawPortions();
      this.updateMaths();
    },

    updateMaths: function () {
      if (!this.cur) return;
      var fb = Calc.financialBreakdown(this.cur.id, this.date, this.state, this.cash, this.online);
      var bc = document.getElementById("billCalc");
      if (bc) {
        var parts = this.state.filter(function (p) { return num(p.liters) > 0; })
          .map(function (p) { return num(p.liters).toFixed(2) + "×₹" + num(p.rate); }).join(" + ");
        bc.textContent = parts || (fb.liters.toFixed(2) + " L");
      }
      var ba = document.getElementById("billAmt"); if (ba) ba.textContent = money(fb.bill);
      var pda = document.getElementById("prevDueAmt"); if (pda) pda.textContent = fb.prevDue >= 0 ? money(fb.prevDue) : "-" + money(Math.abs(fb.prevDue)) + " (Adv)";
      var tra = document.getElementById("totalReqAmt"); if (tra) tra.textContent = money(fb.totalRequired > 0 ? fb.totalRequired : 0);
      var pa = document.getElementById("paidAmt"); if (pa) pa.textContent = money(fb.payment);
      var na = document.getElementById("netAmt"); if (na) na.textContent = money(fb.remainingDue);
      var advRow = document.getElementById("advRow");
      var advAmt = document.getElementById("advAmt");
      if (advRow && advAmt) {
        if (fb.advance > 0) {
          advRow.style.display = "flex";
          advAmt.textContent = money(fb.advance);
        } else {
          advRow.style.display = "none";
        }
      }
      var nr = document.getElementById("netRow");
      if (nr) nr.classList.toggle("zero", fb.remainingDue <= 0);

      var mainInp = document.getElementById("mainMilkInput");
      if (mainInp && document.activeElement !== mainInp) {
        mainInp.value = fb.liters.toFixed(2);
      }

      // Auto Calculation suggestion
      var suggBanner = document.getElementById("suggestedQtyBanner");
      var suggVal = document.getElementById("suggestedQtyVal");
      if (suggBanner && suggVal) {
        if (this.autoCalc) {
          var rate = num(this.cur ? this.cur.rate_per_liter : 0);
          var cash = num(this.cash);
          if (rate > 0 && cash > 0) {
            var sugg = r2(cash / rate);
            suggVal.textContent = sugg.toFixed(2) + " L (" + money(cash) + " ÷ " + money(rate) + "/L)";
            suggBanner.style.display = "flex";
            this._suggestedQty = sugg;
          } else {
            suggBanner.style.display = "none";
            this._suggestedQty = 0;
          }
        } else {
          suggBanner.style.display = "none";
          this._suggestedQty = 0;
        }
      }
    },

    commit: function () {
      var c = this.cur; if (!c) return;
      var portions = this.state.filter(function (p) { return num(p.liters) > 0; });
      if (!portions.length) { toast("कम से कम 0.05 L दूध दर्ज करें", "err"); return; }
      var fb = Calc.financialBreakdown(c.id, this.date, portions, this.cash, this.online);

      // Populate Save Confirmation Sheet (Section 13)
      var d = S.deliveryFor(c.id, this.date);
      var scCust = document.getElementById("scCustName");
      if (scCust) scCust.textContent = c.name + " (" + (c.landmark || c.mohalla || "-") + ")";
      var scMilk = document.getElementById("scMilk");
      if (scMilk) scMilk.textContent = lit(fb.liters);
      var scBill = document.getElementById("scBill");
      if (scBill) scBill.textContent = money(fb.bill);
      var scPrevDue = document.getElementById("scPrevDue");
      if (scPrevDue) scPrevDue.textContent = fb.prevDue >= 0 ? money(fb.prevDue) : "-" + money(Math.abs(fb.prevDue)) + " (Adv)";
      var scPayment = document.getElementById("scPayment");
      if (scPayment) scPayment.textContent = money(fb.payment);
      var scTotalReq = document.getElementById("scTotalReq");
      if (scTotalReq) scTotalReq.textContent = money(fb.totalRequired > 0 ? fb.totalRequired : 0);
      var scRemDue = document.getElementById("scRemDue");
      if (scRemDue) scRemDue.textContent = money(fb.remainingDue);
      var scAdvRow = document.getElementById("scAdvRow");
      var scAdv = document.getElementById("scAdvance");
      if (scAdvRow && scAdv) {
        if (fb.advance > 0) {
          scAdvRow.style.display = "flex";
          scAdv.textContent = money(fb.advance);
        } else {
          scAdvRow.style.display = "none";
        }
      }
      var sni = document.getElementById("saveNoteInput");
      if (sni) sni.value = (d && d.note) || "";

      S.openSheet("saveConfirmSheet");
    },

    naaga: function () {
      var c = this.cur; if (!c) return;
      var date = this.date;
      var db = S.get();
      db.deliveries = db.deliveries.filter(function (d) { return !(d.customer_id === c.id && d.date === date); });
      db.deliveries.push({
        id: uid("d"), customer_id: c.id, date: date, delivery_time: S.timeStr(),
        milk_type: c.milk_type, liters: 0, rate: num(c.rate_per_liter),
        total_amount: 0, paid_cash: 0, paid_online: 0, total_paid: 0,
        status: "skipped", portions_json: "[]"
      });
      S.saveDB(db);
      S.speak(c.name + ", Today skipped, naaga recorded.");
      toast(c.name + " — Naaga", "ok");
      App.refreshAll();
      this.next(true);
    },

    commitFromList: function (c, date) {
      var eff = S.effectivePortions(c);
      var bill = Calc.deliveryBill(eff);
      var l = Calc.deliveryLiters(eff);
      var isCash = c.billing_type === "daily_cash";
      var db = S.get();
      db.deliveries = db.deliveries.filter(function (d) { return !(d.customer_id === c.id && d.date === date); });
      db.deliveries.push({
        id: uid("d"), customer_id: c.id, date: date, delivery_time: S.timeStr(),
        milk_type: eff[0].milk_type || c.milk_type, liters: l,
        rate: l > 0 ? r2(bill / l) : num(c.rate_per_liter), total_amount: bill,
        paid_cash: isCash ? bill : 0, paid_online: 0, total_paid: isCash ? bill : 0,
        status: "delivered", portions_json: JSON.stringify(eff)
      });
      S.saveDB(db);
      S.speak(c.name + ", " + l.toFixed(2) + " Liters milk delivered.");
      toast(c.name + " — " + l.toFixed(2) + " L ✓", "ok");
      App.refreshAll();
    },

    naagaFromList: function (c, date) {
      var db = S.get();
      db.deliveries = db.deliveries.filter(function (d) { return !(d.customer_id === c.id && d.date === date); });
      db.deliveries.push({
        id: uid("d"), customer_id: c.id, date: date, delivery_time: S.timeStr(),
        milk_type: c.milk_type, liters: 0, rate: num(c.rate_per_liter),
        total_amount: 0, paid_cash: 0, paid_online: 0, total_paid: 0, status: "skipped", portions_json: "[]"
      });
      S.saveDB(db); toast(c.name + " — Naaga", "ok"); App.refreshAll();
    },

    next: function (silent) {
      var list = S.routeList(this.date);
      var nextIdx = S.findNextPending(this.idx);
      if (nextIdx >= 0 && nextIdx !== this.idx) { this.idx = nextIdx; this.render(); window.scrollTo(0, 0); return; }
      if (this.idx < list.length - 1) { this.idx++; this.render(); window.scrollTo(0, 0); return; }
      var k = Calc.dailySummary(this.date);
      if (!silent) {
        if (k.pending === 0) App.showSuccess("🎉 Route complete today", k.served + "/" + k.total + " customers • " + k.soldL.toFixed(2) + " L • Bill " + money(k.bill) + " • Cash " + money(k.cash) + " • UPI " + money(k.online), null);
        else toast("End of route — still " + k.pending + " pending", "err");
      }
    },
    prev: function () { if (this.idx > 0) { this.idx--; this.render(); window.scrollTo(0, 0); } },
    jumpTo: function (i) { this.idx = i; this.render(); window.scrollTo(0, 0); },

    wa: function () {
      var c = this.cur; if (!c) return;
      var st = S.settings();
      var due = Calc.customerBalance(c.id);
      var msg = "*Shahbuddin Dairy*\nGulab Nagar, Bareilly\n\nHello " + c.name + " ji,\n" +
        "Today's Milk: " + Calc.deliveryLiters(this.state).toFixed(2) + " L\n" +
        "Today's Bill: " + money(Calc.deliveryBill(this.state)) + "\n" +
        "Previous Due: " + money(this.oldDue) + "\n\n*Total Due: " + money(due) + "*\n\nThank you\nShahbuddin Dairy";
      var ph = (c.phone || "").replace(/[^0-9]/g, "");
      window.location.href = ph ? "whatsapp://send?phone=91" + ph + "&text=" + encodeURIComponent(msg) : "whatsapp://send?text=" + encodeURIComponent(msg);
    }
  };

  /* ================= PDF (canvas, no glyph corruption) ================= */
  var Pdf = {
    make: function () {
      if (typeof html2canvas === "undefined" || typeof jspdf === "undefined") { toast("PDF engine load No done", "err"); return; }
      var date = document.getElementById("reportDate").value || S.todayStr();
      var k = Calc.dailySummary(date);
      var prog = Calc.routeProgress();
      var rows = "";
      var n = 0;
      S.routeList(date).forEach(function (c) {
        n++;
        var d = S.deliveryFor(c.id, date);
        var l = d && d.status === "delivered" ? num(d.liters) : 0;
        var bill = d && d.status === "delivered" ? num(d.total_amount) : 0;
        var cash = d ? Calc.cash(d) : 0, online = d ? Calc.online(d) : 0;
        var st = !d ? "Pending" : (d.status === "delivered" ? "Done" : "Naaga");
        rows += '<tr style="border-bottom:1px solid #E2E8F0;text-align:center;font-size:10.5px">' +
          '<td style="padding:5px 3px">' + n + '</td>' +
          '<td style="padding:5px 3px;text-align:left;font-weight:700">' + esc(c.name) + '</td>' +
          '<td style="padding:5px 3px;text-align:left">' + esc(c.mohalla || "-") + '</td>' +
          '<td style="padding:5px 3px">' + (c.delivery_frequency === "alternate" ? "Alternate" : "Daily") + '</td>' +
          '<td style="padding:5px 3px">' + (l > 0 ? l.toFixed(2) : "—") + '</td>' +
          '<td style="padding:5px 3px">' + (bill > 0 ? money(bill) : "—") + '</td>' +
          '<td style="padding:5px 3px;color:#15803D">' + (cash > 0 ? money(cash) : "—") + '</td>' +
          '<td style="padding:5px 3px;color:#0284C7">' + (online > 0 ? money(online) : "—") + '</td>' +
          '<td style="padding:5px 3px;color:#DC2626;font-weight:700">' + (bill - cash - online > 0 ? money(bill - cash - online) : "—") + '</td>' +
          '<td style="padding:5px 3px">' + st + '</td></tr>';
      });

      var stage = document.getElementById("pdfStage");
      stage.innerHTML = '<div style="border:2px solid #004D40;border-radius:12px;padding:16px">' +
        '<div style="display:flex;justify-content:space-between;align-items:center;border-bottom:2px solid #004D40;padding-bottom:10px">' +
        '<div style="display:flex;align-items:center;gap:12px">' +
        '<div style="width:52px;height:52px;border-radius:50%;background:linear-gradient(135deg,#004D40,#00796B);border:2px solid #D97706;display:flex;align-items:center;justify-content:center;color:#FBBF24;font-weight:900;font-size:19px">SD</div>' +
        '<div><h2 style="margin:0;font-size:21px;color:#004D40">Shahbuddin Dairy</h2>' +
        '<p style="margin:0;font-size:11.5px;color:#64748B">Gulab Nagar, Bareilly • Daily Report</p></div></div>' +
        '<div style="text-align:right;font-size:11.5px"><b>days:</b> ' + S.fmtDate(date) + '<br><b>Time:</b> ' + S.timeStr() + '</div></div>' +
        '<div style="display:grid;grid-template-columns:repeat(5,1fr);gap:8px;margin:12px 0">' +
        '<div style="background:#F0FDF4;border:1px solid #BBF7D0;padding:8px;border-radius:8px;text-align:center"><span style="font-size:9.5px;color:#166534">Milk Sale</span><b style="display:block;font-size:15px;color:#166534">' + k.soldL.toFixed(2) + ' L</b></div>' +
        '<div style="background:#F0FDF4;border:1px solid #BBF7D0;padding:8px;border-radius:8px;text-align:center"><span style="font-size:9.5px;color:#166534">Total Bill</span><b style="display:block;font-size:15px;color:#166534">' + money(k.bill) + '</b></div>' +
        '<div style="background:#F8FAFC;border:1px solid #E2E8F0;padding:8px;border-radius:8px;text-align:center"><span style="font-size:9.5px">Cash</span><b style="display:block;font-size:15px;color:#15803D">' + money(k.cash) + '</b></div>' +
        '<div style="background:#F8FAFC;border:1px solid #E2E8F0;padding:8px;border-radius:8px;text-align:center"><span style="font-size:9.5px">UPI</span><b style="display:block;font-size:15px;color:#0284C7">' + money(k.online) + '</b></div>' +
        '<div style="background:#FEF2F2;border:1px solid #FECACA;padding:8px;border-radius:8px;text-align:center"><span style="font-size:9.5px;color:#991B1B">New Due</span><b style="display:block;font-size:15px;color:#DC2626">' + money(k.credit) + '</b></div></div>' +
        '<table style="width:100%;border-collapse:collapse;margin-top:6px"><thead><tr style="background:#004D40;color:#fff;font-size:10.5px">' +
        '<th style="padding:5px">#</th><th style="padding:5px;text-align:left">Customer</th><th style="padding:5px;text-align:left">Area</th><th style="padding:5px">Schedule</th>' +
        '<th style="padding:5px">Liters</th><th style="padding:5px">Bill</th><th style="padding:5px">Cash</th><th style="padding:5px">UPI</th><th style="padding:5px">Due</th><th style="padding:5px">Status</th>' +
        '</tr></thead><tbody>' + rows + '</tbody></table>' +
        '<div style="margin-top:9px;font-size:10.5px;color:#64748B">भैंस: ' + k.buffalo.toFixed(2) + ' L | गाय: ' + k.cow.toFixed(2) + ' L | Done: ' + k.served + ' | Naaga: ' + k.naaga + ' | Pending: ' + k.pending + ' | Route: ' + prog.pct + '%</div>' +
        '<div style="text-align:center;margin-top:8px;font-size:11.5px;font-weight:800;color:#004D40">Shahbuddin Dairy, Gulab Nagar, Bareilly</div></div>';

      toast("PDF ready be is is...");
      html2canvas(stage, { scale: 2, backgroundColor: "#FFFFFF", useCORS: true }).then(function (canvas) {
        var img = canvas.toDataURL("image/jpeg", 0.95);
        var pdf = new jspdf.jsPDF("p", "mm", "a4");
        var w = 210, h = (canvas.height * w) / canvas.width;
        pdf.addImage(img, "JPEG", 0, 5, w, h);
        pdf.save("ShahbuddinDairy_Report_" + date + ".pdf");
        toast("A4 PDF download be done ✓", "ok");
      }).catch(function () { toast("PDF render in error", "err"); });
    }
  };

  /* ================= LOCAL HELPERS (migration normalizers) ================= */
  function normalizeLocal(c, i) {
    return {
      id: num(c.id, i + 1), name: c.name || "", phone: c.phone || "",
      mohalla: c.mohalla || "कटरा", landmark: c.landmark || "", note: c.note || "",
      milk_type: c.milk_type || "भैंस", default_liters: num(c.default_liters, 1),
      rate_per_liter: num(c.rate_per_liter, 0),
      billing_type: c.billing_type === "daily_cash" ? "daily_cash" : "monthly",
      delivery_frequency: c.delivery_frequency === "alternate" ? "alternate" : "daily",
      frequency_anchor_date: String(c.frequency_anchor_date || c.created_at || S.todayStr()).slice(0, 10),
      is_active: num(c.is_active, 1) === 0 ? 0 : 1,
      route_order: num(c.route_order, i + 1),
      portions_json: c.portions_json || "[]",
      created_at: c.created_at || new Date().toISOString()
    };
  }
  function normalizeDeliveryLocal(d) {
    var cash = d.paid_cash != null ? num(d.paid_cash) : num(d.paid_amount);
    var online = num(d.paid_online);
    return {
      id: d.id || uid("d"), customer_id: num(d.customer_id), date: String(d.date || "").slice(0, 10),
      delivery_time: d.delivery_time || "", milk_type: d.milk_type || "भैंस",
      liters: num(d.liters), rate: num(d.rate), total_amount: num(d.total_amount),
      paid_cash: cash, paid_online: online,
      total_paid: d.total_paid != null ? num(d.total_paid) : r2(cash + online),
      status: d.status === "skipped" ? "skipped" : "delivered",
      portions_json: d.portions_json || "[]"
    };
  }
  function normalizePaymentLocal(p) {
    var mode = p.payment_mode || "Cash";
    return {
      id: p.id || uid("pay"), customer_id: num(p.customer_id), date: String(p.date || "").slice(0, 10),
      amount: num(p.amount), payment_mode: mode, payment_subtype: p.payment_subtype || (mode === "Cash" ? "" : mode),
      note: p.note || "Payment", created_at: p.created_at || new Date().toISOString()
    };
  }
  function safeParseLocal(s, fb) { try { var v = JSON.parse(s); return v == null ? fb : v; } catch (e) { return fb; } }
  function r0(v) { return Math.round(num(v)); }

  /* ================= WIRE UP ================= */
  window.App = App;
  window.Runner = Runner;
  App.bindSaleClicks();
  App.bindCustomerList();

  document.addEventListener("DOMContentLoaded", function () { App.init(); });
})();
