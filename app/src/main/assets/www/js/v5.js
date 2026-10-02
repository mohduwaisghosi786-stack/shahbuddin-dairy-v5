(function () {
  "use strict";
  var S = window.__SHD;
  if (!S) return;
  var Calc = S.Calc;
  var num = S.num, r2 = S.r2, money = S.money, esc = S.esc, uid = S.uid;
  var toast = S.toast;

  var V5KEY = "shahbuddindairy_v5_meta";
  var BACKUP_FORMAT = 1.0;

  function meta() {
    try { return JSON.parse(localStorage.getItem(V5KEY)) || {}; } catch (e) { return {}; }
  }
  function saveMeta(m) { try { localStorage.setItem(V5KEY, JSON.stringify(m)); } catch (e) {} }

  var V5 = {
    init: function () {
      try { this.injectUI(); this.bind(); } catch (e) { console.error("V5 init", e); }
    },

    injectUI: function () {
      var accounts = document.getElementById("view-accounts");
      if (!accounts) return;

      var followup = document.createElement("div");
      followup.className = "panel";
      followup.innerHTML =
        '<h3 class="panel-title">\uD83D\uDCB0 Payment Follow-up</h3>' +
        '<div id="v5FollowList"></div>' +
        '<button class="btn btn-wa btn-block" id="v5RemindAll">\uD83D\uDCF2 Remind All (highest first)</button>';
      accounts.appendChild(followup);

      var area = document.createElement("div");
      area.className = "panel";
      area.innerHTML =
        '<h3 class="panel-title">\uD83D\uDCCD Area Dashboard</h3>' +
        '<div id="v5AreaList"></div>';
      accounts.insertBefore(area, followup);

      var tools = document.createElement("div");
      tools.className = "panel";
      tools.innerHTML =
        '<h3 class="panel-title">\uD83D\uDCCD Nearby / GPS</h3>' +
        '<button class="btn btn-secondary btn-block" id="v5NearbyBtn">\uD83D\uDCCD Nearby Customers</button>' +
        '<div id="v5NearbyList" class="hint"></div>' +
        '<div class="form-row" style="margin-top:10px"><label>Arrival radius</label>' +
        '<select id="v5Radius" class="inp"><option value="50">50 m</option><option value="100" selected>100 m</option><option value="200">200 m</option></select></div>';
      accounts.insertBefore(tools, area);

      var audit = document.createElement("div");
      audit.className = "panel";
      audit.innerHTML = '<h3 class="panel-title">\uD83D\uDCDC Audit History</h3><div id="v5AuditList" style="max-height:260px;overflow:auto"></div>';
      accounts.insertBefore(audit, tools);

      var notif = document.createElement("div");
      notif.className = "panel";
      notif.innerHTML =
        '<h3 class="panel-title">\uD83D\uDD14 Notifications</h3>' +
        '<div class="form-row"><label>Route Reminder</label><select id="v5nRoute" class="inp"><option value="1">ON</option><option value="0">OFF</option></select></div>' +
        '<div class="form-row"><label>Pending Reminder</label><select id="v5nPending" class="inp"><option value="1">ON</option><option value="0">OFF</option></select></div>' +
        '<div class="form-row"><label>Closing Reminder</label><select id="v5nClosing" class="inp"><option value="1">ON</option><option value="0">OFF</option></select></div>' +
        '<div class="form-row"><label>Backup Reminder</label><select id="v5nBackup" class="inp"><option value="1">ON</option><option value="0">OFF</option></select></div>' +
        '<button class="btn btn-secondary btn-block" id="v5SaveNotif">Save Notifications</button>';
      accounts.insertBefore(notif, audit);

      var dayPanel = document.createElement("div");
      dayPanel.className = "panel";
      dayPanel.id = "v5DayPanel";
      var report = document.getElementById("view-report");
      if (report) {
        var actions = report.querySelector(".rep-actions");
        if (actions) {
          var b = document.createElement("button");
          b.className = "btn btn-primary";
          b.id = "v5CloseDayBtn";
          b.textContent = "\uD83D\uDD12 Close Day";
          actions.appendChild(b);
          var mb = document.createElement("button");
          mb.className = "btn btn-outline";
          mb.id = "v5MonthBillBtn";
          mb.textContent = "\uD83D\uDCC4 Monthly Bill";
          actions.appendChild(mb);
        }
      }
    },

    bind: function () {
      var self = this;
      var byId = function (id) { return document.getElementById(id); };
      function on(id, ev, fn) { var e = byId(id); if (e) e.addEventListener(ev, fn); }

      on("v5BackupNow", "click", function () { self.backupNow(); });
      on("v5RecoveryPkg", "click", function () { self.backupNow(true); });
      on("v5RestoreFile", "change", function (e) { self.restore(e); });
      on("v5VerifyFile", "change", function (e) { self.verify(e); });
      on("v5RemindAll", "click", function () { self.remindAll(); });
      on("v5NearbyBtn", "click", function () { self.nearby(); });
      on("v5SaveNotif", "click", function () { self.saveNotif(); });
      on("v5CloseDayBtn", "click", function () { self.closeDay(); });
      on("v5MonthBillBtn", "click", function () { self.monthBillPrompt(); });
      this.refresh();
    },

    refresh: function () {
      this.renderFollow();
      this.renderArea();
      this.renderAudit();
      this.renderBackupInfo();
      this.loadNotif();
    },

    /* ============ BACKUP ============ */
    buildPackage: function (full) {
      var db = S.get();
      var st = S.settings();
      var m = meta();
      var payload = {
        manifest: {
          app: "ShahbuddinDairy",
          app_version: "5.0.0",
          backup_format: BACKUP_FORMAT,
          schema: 5,
          created_at: new Date().toISOString(),
          customers: (db.customers || []).length,
          deliveries: (db.deliveries || []).length,
          payments: (db.payments || []).length
        },
        database: {
          customers: db.customers || [],
          deliveries: db.deliveries || [],
          payments: db.payments || [],
          procurement: db.procurement || []
        },
        settings: st,
        routes: (db.customers || []).map(function (c) { return { id: c.id, route_order: c.route_order, area: c.mohalla || "" }; }),
        audit: m.audit || [],
        credits: db.credits || [],
        notifications: m.notif || {},
        closing: m.closing || {},
        migration: m.migration || {}
      };
      payload.manifest.checksum = this.checksum(JSON.stringify(payload.database) + JSON.stringify(payload.settings));
      return payload;
    },

    checksum: function (str) {
      var h = 5381;
      for (var i = 0; i < str.length; i++) { h = ((h << 5) + h + str.charCodeAt(i)) | 0; }
      return (h >>> 0).toString(16);
    },

    backupNow: function (full) {
      if (window.BackupSystem) {
        window.BackupSystem.exportBackup();
      } else {
        var pkg = this.buildPackage(full);
        var str = JSON.stringify(pkg);
        var name = "ShahbuddinDairy_" + (full ? "FullBackup_" : "Backup_") + S.todayStr().replace(/-/g, "-") + ".sdbbackup";
        S.downloadUri("data:application/json;charset=utf-8;base64," + S.encode(str), name);
        toast("Backup created — saving to Downloads", "ok", 3200);
      }
      var m = meta(); m.lastBackup = new Date().toISOString(); m.backupCount = (m.backupCount || 0) + 1; saveMeta(m);
      this.audit("Backup Created", "backup", "Full export");
      this.renderAudit();
    },

    renderBackupInfo: function () {
      var m = meta();
      var el = document.getElementById("v5BackupInfo");
      if (!el) return;
      var db = S.get();
      var last = m.lastBackup ? new Date(m.lastBackup).toLocaleString() : "\u2014";
      el.innerHTML = "Last Backup: <b>" + esc(last) + "</b><br>Customers: <b>" + (db.customers || []).length +
        "</b> \u2022 Deliveries: <b>" + (db.deliveries || []).length + "</b> \u2022 Payments: <b>" + (db.payments || []).length + "</b>";
    },

    readFile: function (file, cb) {
      var r = new FileReader();
      r.onload = function (e) { cb(e.target.result); };
      r.readAsText(file);
    },

    analyze: function (raw) {
      var data = null;
      try { data = JSON.parse(raw); } catch (e) { return { ok: false, reason: "Invalid JSON" }; }
      if (!data) return { ok: false, reason: "Empty" };
      var manifest = data.manifest || null;
      var db = data.database || null;
      if (!db && data.app === "ShahbuddinDairy" && Array.isArray(data.customers)) {
        manifest = { app_version: "legacy", backup_format: 0, schema: data.schema || 3, customers: data.customers.length, deliveries: (data.deliveries || []).length, payments: (data.payments || []).length };
        db = { customers: data.customers, deliveries: data.deliveries || [], payments: data.payments || [], procurement: data.daily_procurement || data.procurement || [] };
      }
      if (!db || !Array.isArray(db.customers)) return { ok: false, reason: "No customer data" };
      var checksumOk = true;
      if (manifest && manifest.checksum) {
        var expect = this.checksum(JSON.stringify(db) + JSON.stringify(data.settings || {}));
        checksumOk = (expect === manifest.checksum);
      }
      var migration = (manifest && manifest.schema && manifest.schema < 5);
      return { ok: true, manifest: manifest, db: db, settings: data.settings || {}, audit: data.audit || [], credits: data.credits || [], checksumOk: checksumOk, migration: migration, raw: data };
    },

    verify: function (ev) {
      var f = ev.target.files && ev.target.files[0]; if (!f) return;
      var self = this;
      this.readFile(f, function (raw) {
        var a = self.analyze(raw);
        var el = document.getElementById("v5BackupResult");
        if (!a.ok) { if (el) el.innerHTML = "<b style='color:var(--danger)'>\u26A0 Backup has problems</b><br>" + esc(a.reason); toast("Backup \u0917\u0932\u0924 \u0939\u0948", "err"); ev.target.value = ""; return; }
        var m = a.manifest || {};
        var lines = ["<b>\u2705 Backup is healthy</b>",
          "Created: " + esc(m.created_at || "\u2014"),
          "Source App: " + esc(m.app_version || "legacy") + " \u2022 Format: " + esc(String(m.backup_format || 0)) + " \u2022 Schema: " + esc(String(m.schema || "?")) ,
          "Customers: " + (a.db.customers.length) + " \u2022 Deliveries: " + (a.db.deliveries.length) + " \u2022 Payments: " + (a.db.payments.length),
          (a.checksumOk ? "\u2713 Checksum Valid" : "\u26A0 Checksum mismatch"),
          (a.migration ? "\u2713 Migration available (schema " + m.schema + " \u2192 5)" : "\u2713 Schema recognized")];
        if (el) el.innerHTML = lines.join("<br>");
        toast("Backup healthy \u2713", "ok");
        ev.target.value = "";
      });
    },

    restore: function (ev) {
      var f = ev.target.files && ev.target.files[0]; if (!f) return;
      var self = this;
      this.readFile(f, function (raw) {
        var a = self.analyze(raw);
        if (!a.ok) { toast("Backup \u0917\u0932\u0924 \u0939\u0948: " + a.reason, "err", 3500); ev.target.value = ""; return; }
        if (a.checksumOk === false && !confirm("\u26A0 Checksum mismatch. Phir bhi restore karein?")) { ev.target.value = ""; return; }
        if (a.migration && !confirm("Old backup detected (schema " + (a.manifest.schema) + " \u2192 5).\nMigration required. Original file modify nahi hoga.\nContinue?")) { ev.target.value = ""; return; }
        if (!confirm("Current data will be replaced. An emergency backup will be taken first. Continue?")) { ev.target.value = ""; return; }
        // emergency backup
        try { self.backupNow(); } catch (e) {}
        var live = S.get();
        var rollback = { customers: live.customers, deliveries: live.deliveries, payments: live.payments, procurement: live.procurement };
        try {
          var migrated = self.migrate(a);
          S.set(migrated.db);
          if (a.settings && Object.keys(a.settings).length) S.setSettings(a.settings);
          var m = meta();
          m.audit = a.audit && a.audit.length ? a.audit : (m.audit || []);
          m.migration = { last_from: a.manifest && a.manifest.schema, at: new Date().toISOString() };
          saveMeta(m);
          self.audit("Restore", "restore", a.db.customers.length + " customers, " + a.db.deliveries.length + " deliveries");
          if (window.App && App.refreshAll) App.refreshAll();
          self.refresh();
          var el = document.getElementById("v5BackupResult");
          if (el) el.innerHTML = "<b>\u2705 Restore Complete</b><br>Customers: " + a.db.customers.length + " \u2022 Deliveries: " + a.db.deliveries.length + " \u2022 Payments: " + a.db.payments.length;
          toast("Restore complete \u2713", "ok", 3200);
        } catch (e) {
          S.set({ customers: rollback.customers, deliveries: rollback.deliveries, payments: rollback.payments, procurement: rollback.procurement });
          toast("Restore \u0935\u093F\u092B\u0932 \u2014 \u092A\u0941\u0930\u093E\u0928\u093E \u0921\u0947\u091F\u093E \u0935\u093E\u092A\u0938", "err", 3500);
        }
        ev.target.value = "";
      });
    },

    migrate: function (a) {
      var db = { customers: [], deliveries: [], payments: [], procurement: [] };
      var src = a.db;
      (src.customers || []).forEach(function (c, i) {
        db.customers.push({
          id: num(c.id, i + 1), name: c.name || "", phone: c.phone || "", mohalla: c.mohalla || "\u0915\u091F\u0930\u093E",
          landmark: c.landmark || "", note: c.note || "", milk_type: c.milk_type || "\u092D\u0948\u0902\u0938",
          default_liters: num(c.default_liters, 1), rate_per_liter: num(c.rate_per_liter, 0),
          billing_type: c.billing_type === "daily_cash" ? "daily_cash" : "monthly",
          delivery_frequency: c.delivery_frequency === "alternate" ? "alternate" : "daily",
          frequency_anchor_date: String(c.frequency_anchor_date || c.created_at || S.todayStr()).slice(0, 10),
          is_active: num(c.is_active, 1) === 0 ? 0 : 1, route_order: num(c.route_order, i + 1),
          lat: c.lat != null ? num(c.lat) : null, lng: c.lng != null ? num(c.lng) : null,
          loc_acc: c.loc_acc != null ? num(c.loc_acc) : null, loc_at: c.loc_at || null,
          advance: num(c.advance, 0),
          portions_json: c.portions_json || "[]", created_at: c.created_at || new Date().toISOString()
        });
      });
      (src.deliveries || []).forEach(function (d) {
        var cash = d.paid_cash != null ? num(d.paid_cash) : num(d.paid_amount);
        var online = num(d.paid_online);
        if (!d.customer_id) return;
        db.deliveries.push({
          id: d.id || uid("d"), customer_id: num(d.customer_id), date: String(d.date || "").slice(0, 10),
          delivery_time: d.delivery_time || "", milk_type: d.milk_type || "\u092D\u0948\u0902\u0938",
          liters: num(d.liters), rate: num(d.rate), total_amount: num(d.total_amount),
          paid_cash: cash, paid_online: online,
          total_paid: d.total_paid != null ? num(d.total_paid) : r2(cash + online),
          status: d.status === "skipped" ? "skipped" : "delivered", portions_json: d.portions_json || "[]"
        });
      });
      (src.payments || []).forEach(function (p) {
        if (!p.customer_id) return;
        var mode = p.payment_mode || "Cash";
        db.payments.push({
          id: p.id || uid("pay"), customer_id: num(p.customer_id), date: String(p.date || "").slice(0, 10),
          amount: num(p.amount), payment_mode: mode, payment_subtype: p.payment_subtype || (mode === "Cash" ? "" : mode),
          note: p.note || "\u091C\u092E\u093E", created_at: p.created_at || new Date().toISOString()
        });
      });
      db.procurement = (src.procurement || []).slice();
      return { db: db };
    },

    /* ============ AUDIT ============ */
    audit: function (action, entity, detail) {
      var m = meta();
      m.audit = m.audit || [];
      m.audit.unshift({ at: new Date().toISOString(), action: action, entity: entity || "", detail: detail || "" });
      if (m.audit.length > 500) m.audit = m.audit.slice(0, 500);
      saveMeta(m);
    },

    renderAudit: function () {
      var el = document.getElementById("v5AuditList");
      if (!el) return;
      var m = meta();
      var list = m.audit || [];
      if (!list.length) { el.innerHTML = '<div class="hint">No audit entries.</div>'; return; }
      el.innerHTML = list.slice(0, 60).map(function (a) {
        var dt = new Date(a.at);
        return '<div class="ledger-item2" style="padding:8px 10px;margin-bottom:6px">' +
          '<div class="ledger-top"><b>' + esc(a.action) + '</b><span class="ledger-time">' + esc(dt.toLocaleString()) + '</span></div>' +
          (a.detail ? '<div class="ledger-mid">' + esc(a.detail) + '</div>' : "") + '</div>';
      }).join("");
    },

    /* ============ FOLLOW-UP ============ */
    renderFollow: function () {
      var el = document.getElementById("v5FollowList");
      if (!el) return;
      var list = S.sorted().map(function (c) { return { c: c, due: Calc.customerBalance(c.id) }; })
        .filter(function (x) { return x.due > 0; })
        .sort(function (a, b) { return b.due - a.due; });
      if (!list.length) { el.innerHTML = '<div class="hint">\u2705 No outstanding balances.</div>'; return; }
      var total = 0; list.forEach(function (x) { total += x.due; });
      el.innerHTML = '<div class="hint" style="margin-bottom:8px">Total Outstanding: <b style="color:var(--danger)">' + money(total) + '</b></div>' +
        list.slice(0, 40).map(function (x, i) {
          var n = i + 1 < 10 ? "0" + (i + 1) : (i + 1);
          return '<div class="ledger-item2" style="padding:9px 11px;margin-bottom:7px">' +
            '<div class="ledger-top"><b>' + n + '. ' + esc(x.c.name) + '</b><span class="ledger-bot bal" style="color:var(--danger);font-weight:800">' + money(x.due) + '</span></div>' +
            '<div class="ccard-actions" style="margin-top:7px">' +
            (x.c.phone ? '<button class="icon-btn" data-v5call="' + esc(x.c.phone) + '">\uD83D\uDCDE</button>' : "") +
            '<button class="icon-btn" data-v5remind="' + x.c.id + '">\uD83D\uDCF2</button>' +
            '<button class="icon-btn" data-v5collect="' + x.c.id + '">\uD83D\uDCB5</button>' +
            '<button class="icon-btn" data-v5khata="' + x.c.id + '">\uD83D\uDCD6</button>' +
            '</div></div>';
        }).join("");
    },

    bindFollowClicks: function () {
      var self = this;
      var el = document.getElementById("v5FollowList");
      if (!el || el._bound) return;
      el._bound = true;
      el.addEventListener("click", function (e) {
        var t = e.target.closest("[data-v5call],[data-v5remind],[data-v5collect],[data-v5khata]");
        if (!t) return;
        if (t.hasAttribute("data-v5call")) { window.location.href = "tel:" + t.getAttribute("data-v5call").replace(/[^0-9+]/g, ""); return; }
        var id = parseInt(t.getAttribute("data-v5remind") || t.getAttribute("data-v5collect") || t.getAttribute("data-v5khata"), 10);
        if (t.hasAttribute("data-v5remind")) self.remind(id);
        else if (t.hasAttribute("data-v5collect")) self.collect(id);
        else if (t.hasAttribute("data-v5khata") && window.App) window.App.openKhata(id);
      });
    },

    remind: function (id) {
      var c = S.getCustomer(id); if (!c) return;
      var due = Calc.customerBalance(id);
      var msg = "\u0928\u092E\u0938\u094D\u0924\u0947 " + c.name + " \u091C\u0940,\n\n\u0906\u092A\u0915\u093E \u0935\u0930\u094D\u0924\u092E\u093E\u0928 \u092C\u0915\u093E\u092F\u093E " + money(due) + " \u0939\u0948\u0964\n\n\u0915\u0943\u092A\u092F\u093E \u0938\u0941\u0935\u093F\u0927\u093E \u0905\u0928\u0941\u0938\u093E\u0930 \u092D\u0941\u0917\u0924\u093E\u0928 \u0915\u0930 \u0926\u0947\u0902\u0964\n\n\u0936\u093E\u0939\u092C\u0941\u0926\u094D\u0926\u0940\u0928 \u0921\u0947\u092F\u0930\u0940\n\u0917\u0941\u0932\u093E\u092C \u0928\u0917\u0930, \u092C\u0930\u0947\u0932\u0940";
      var ph = (c.phone || "").replace(/[^0-9]/g, "");
      window.location.href = ph ? "whatsapp://send?phone=91" + ph + "&text=" + encodeURIComponent(msg) : "whatsapp://send?text=" + encodeURIComponent(msg);
      this.audit("WhatsApp Reminder", "customer", c.name + " \u20B9" + due);
    },

    remindAll: function () {
      var list = S.sorted().map(function (c) { return { c: c, due: Calc.customerBalance(c.id) }; }).filter(function (x) { return x.due > 0; }).sort(function (a, b) { return b.due - a.due; });
      if (!list.length) { toast("Koi bakaya nahi", "ok"); return; }
      this.remind(list[0].c.id);
    },

    collect: function (id) {
      var c = S.getCustomer(id); if (!c) return;
      var due = Calc.customerBalance(id);
      var amt = prompt("\u0928\u0915\u0926/\u0911\u0928\u0932\u093E\u0907\u0928 \u091C\u092E\u093E \u0930\u093E\u0936\u093F \u20B9 (Due " + Math.round(due) + ")", Math.round(due > 0 ? due : 0));
      if (amt == null) return;
      var v = num(amt, 0); if (v <= 0) return;
      var mode = confirm("OK = Cash, Cancel = UPI/Online") ? "Cash" : "UPI";
      var db = S.get();
      db.payments.push({ id: uid("pay"), customer_id: c.id, date: S.todayStr(), amount: r2(v), payment_mode: mode, payment_subtype: mode, note: mode + " \u091C\u092E\u093E", created_at: new Date().toISOString() });
      S.saveDB(db);
      this.audit("Payment Collected", "customer", c.name + " " + money(v) + " (" + mode + ")");
      toast(money(v) + " \u091C\u092E\u093E", "ok");
      if (window.App && App.refreshAll) App.refreshAll();
      this.refresh();
      this.receipt(c.id, v, mode);
    },

    receipt: function (id, amt, mode) {
      var c = S.getCustomer(id); if (!c) return;
      var due = Calc.customerBalance(id);
      var prev = r2(due + amt);
      var txt = "PAYMENT RECEIPT\n\n" + c.name + "\n" + S.fmtDate(S.todayStr()) + " \u2022 " + S.timeStr() + "\n\nReceived: " + money(amt) + "\nMode: " + mode + "\n\nPrevious Due: " + money(prev) + "\nPayment: " + money(amt) + "\nRemaining: " + money(due) + "\n\nShahbuddin Dairy\nGulab Nagar, Bareilly";
      if (confirm("Receipt bhejein?\n\n" + txt)) {
        var ph = (c.phone || "").replace(/[^0-9]/g, "");
        window.location.href = ph ? "whatsapp://send?phone=91" + ph + "&text=" + encodeURIComponent(txt) : "whatsapp://send?text=" + encodeURIComponent(txt);
      }
    },

    /* ============ AREA ============ */
    renderArea: function () {
      var el = document.getElementById("v5AreaList");
      if (!el) return;
      var counts = {};
      S.sorted().forEach(function (c) { if (num(c.is_active, 1) === 1) { var a = c.mohalla || "Other"; counts[a] = (counts[a] || 0) + 1; } });
      var keys = Object.keys(counts).sort(function (a, b) { return counts[b] - counts[a]; });
      if (!keys.length) { el.innerHTML = '<div class="hint">No customers.</div>'; return; }
      el.innerHTML = keys.map(function (a) {
        return '<div class="ledger-top" style="padding:7px 0;border-bottom:1px solid var(--border)"><b>' + esc(a) + '</b><span>' + counts[a] + '</span></div>';
      }).join("");
    },

    /* ============ NEARBY / GPS ============ */
    nearby: function () {
      var self = this;
      if (!navigator.geolocation) { toast("GPS uplabdh nahi", "err"); return; }
      var radius = num((document.getElementById("v5Radius") || {}).value, 100);
      toast("Location le rahe hain...");
      navigator.geolocation.getCurrentPosition(function (pos) {
        var me = { lat: pos.coords.latitude, lng: pos.coords.longitude };
        var rows = [];
        S.sorted().forEach(function (c) {
          if (c.lat == null || c.lng == null) return;
          var d = self.distance(me.lat, me.lng, num(c.lat), num(c.lng));
          rows.push({ c: c, d: d });
        });
        rows.sort(function (a, b) { return a.d - b.d; });
        var el = document.getElementById("v5NearbyList");
        var near = rows.filter(function (r) { return r.d <= radius; });
        el.innerHTML = near.length ? near.map(function (r) {
          return '<div class="ledger-top" style="padding:7px 0;border-bottom:1px solid var(--border)"><b>' + Math.round(r.d) + ' m</b><span>' + esc(r.c.name) + '</span></div>';
        }).join("") : ('<div class="hint">No customers within ' + radius + ' m. (' + rows.length + ' with saved location)</div>');
        toast(near.length + " nearby", "ok");
      }, function () { toast("Location permission nahi mili", "err"); }, { enableHighAccuracy: true, timeout: 12000, maximumAge: 30000 });
    },

    distance: function (lat1, lon1, lat2, lon2) {
      var R = 6371000, toRad = function (d) { return d * Math.PI / 180; };
      var dLat = toRad(lat2 - lat1), dLon = toRad(lon2 - lon1);
      var a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
      return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    },

    saveCustomerLocation: function (id) {
      var c = S.getCustomer(id); if (!c) return;
      if (!navigator.geolocation) { toast("GPS nahi", "err"); return; }
      toast("Location save ho rahi...");
      navigator.geolocation.getCurrentPosition(function (pos) {
        c.lat = pos.coords.latitude; c.lng = pos.coords.longitude;
        c.loc_acc = pos.coords.accuracy; c.loc_at = new Date().toISOString();
        S.saveDB(S.get());
        toast("Location saved \u2713", "ok");
      }, function () { toast("Location nahi mili", "err"); }, { enableHighAccuracy: true, timeout: 12000 });
    },

    /* ============ NOTIFICATIONS ============ */
    saveNotif: function () {
      var m = meta();
      m.notif = {
        route: (document.getElementById("v5nRoute") || {}).value === "1",
        pending: (document.getElementById("v5nPending") || {}).value === "1",
        closing: (document.getElementById("v5nClosing") || {}).value === "1",
        backup: (document.getElementById("v5nBackup") || {}).value === "1"
      };
      saveMeta(m);
      toast("Notifications saved", "ok");
    },
    loadNotif: function () {
      var n = meta().notif || {};
      function set(id, v) { var e = document.getElementById(id); if (e) e.value = (v === false ? "0" : "1"); }
      set("v5nRoute", n.route); set("v5nPending", n.pending); set("v5nClosing", n.closing); set("v5nBackup", n.backup);
    },

    /* ============ CLOSE DAY / MONTH ============ */
    closeDay: function () {
      var date = (document.getElementById("reportDate") || {}).value || S.todayStr();
      var k = Calc.dailySummary(date);
      var msg = "Close Day " + S.fmtDate(date) + "?\n\nDelivered: " + k.served + "\nNaaga: " + k.naaga + "\nPending: " + k.pending +
        "\nMilk: " + k.soldL.toFixed(2) + " L\nBills: " + money(k.bill) + "\nCash: " + money(k.cash) + "\nUPI: " + money(k.online);
      if (k.pending > 0) msg += "\n\n\u26A0 " + k.pending + " customers still pending!";
      if (!confirm(msg + "\n\nConfirm close?")) return;
      var m = meta(); m.closing = m.closing || {}; m.closing[date] = { at: new Date().toISOString(), k: k }; saveMeta(m);
      this.audit("Day Closed", "day", S.fmtDate(date) + " \u2022 " + k.served + "/" + k.total);
      toast("Day closed \u2713", "ok");
      this.refresh();
    },

    monthBillPrompt: function () {
      var idStr = prompt("Customer ID (khata se) ya naam ka pehla hissa?");
      if (idStr == null) return;
      var c = null;
      var asId = parseInt(idStr, 10);
      if (!isNaN(asId)) c = S.getCustomer(asId);
      if (!c) { var q = String(idStr).toLowerCase(); S.sorted().forEach(function (x) { if (!c && x.name.toLowerCase().indexOf(q) !== -1) c = x; }); }
      if (!c) { toast("Customer nahi mila", "err"); return; }
      this.monthBill(c.id);
    },

    monthBill: function (id) {
      var c = S.getCustomer(id); if (!c) return;
      var d = new Date(); var ym = d.getFullYear() + "-" + (d.getMonth() + 1 < 10 ? "0" : "") + (d.getMonth() + 1);
      var db = S.get();
      var lit = 0, bill = 0, cash = 0, online = 0;
      db.deliveries.forEach(function (x) {
        if (x.customer_id !== c.id || String(x.date).indexOf(ym) !== 0) return;
        if (x.status === "delivered") { lit += num(x.liters); bill += num(x.total_amount); }
        cash += Calc.cash(x); online += Calc.online(x);
      });
      db.payments.forEach(function (p) { if (p.customer_id === c.id && String(p.date).indexOf(ym) === 0) { if (p.payment_mode === "Cash") cash += num(p.amount); else online += num(p.amount); } });
      var due = Calc.customerBalance(c.id);
      var txt = "*\u0936\u093E\u0939\u092C\u0941\u0926\u094D\u0926\u0940\u0928 \u0921\u0947\u092F\u0930\u0940*\n" + c.name + " \u2014 " + ym + "\n\n" +
        "\u0915\u0941\u0932 \u0926\u0942\u0927: " + r2(lit).toFixed(2) + " L\n\u0915\u0941\u0932 \u092C\u093F\u0932: " + money(bill) +
        "\n\u0928\u0915\u0926: " + money(cash) + "\n\u0911\u0928\u0932\u093E\u0907\u0928: " + money(online) + "\n\n*\u0915\u0941\u0932 \u092C\u0915\u093E\u092F\u093E: " + money(due) + "*";
      var ph = (c.phone || "").replace(/[^0-9]/g, "");
      window.location.href = ph ? "whatsapp://send?phone=91" + ph + "&text=" + encodeURIComponent(txt) : "whatsapp://send?text=" + encodeURIComponent(txt);
      this.audit("Monthly Bill", "customer", c.name + " " + ym);
    }
  };

  window.V5 = V5;
  document.addEventListener("DOMContentLoaded", function () {
    setTimeout(function () { V5.init(); V5.bindFollowClicks(); }, 400);
  });
})();
