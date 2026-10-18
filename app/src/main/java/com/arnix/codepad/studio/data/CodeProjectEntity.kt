package com.arnix.codepad.studio.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CodeTab(
    val index: Int,
    val fileName: String,
    val languageTag: String,
    val extension: String
) {
    INDEX_HTML(0, "index.html", "HTML", "html"),
    STYLE_CSS(1, "style.css", "CSS", "css"),
    SCRIPT_JS(2, "script.js", "JavaScript", "js");

    companion object {
        fun fromIndex(index: Int): CodeTab = entries.firstOrNull { it.index == index } ?: INDEX_HTML
    }
}

@Entity(tableName = "code_projects")
data class CodeProjectEntity(
    @PrimaryKey val id: Int = 1,
    val projectName: String = "Portfolio Card",
    val htmlCode: String = ProjectTemplates.PORTFOLIO_HTML,
    val cssCode: String = ProjectTemplates.PORTFOLIO_CSS,
    val jsCode: String = ProjectTemplates.PORTFOLIO_JS,
    val editorFontSize: Float = 11.5f,
    val autoWriteFontSize: Float = 11f,
    val uiFontScale: Float = 0.92f,
    val activeTabIndex: Int = 0,
    val isDarkTheme: Boolean = false,
    // Persisted Creator Studio Preferences
    val creatorHandle: String = "@codewitharnix",
    val watermarkPositionOrdinal: Int = 0,
    val watermarkStyleOrdinal: Int = 0,
    val autoDurationTargetOrdinal: Int = 4, // SEC_60 (1m Shorts)
    val speedLevel: Int = 14,
    val isTurboMode: Boolean = false,
    val previewSectionHeightDp: Float = 195f,
    val showEndCtaAnimation: Boolean = true,
    val endCtaStyleOrdinal: Int = 1, // SUBSCRIBE_CARD
    val endCtaPositionOrdinal: Int = 0, // CODE_BOTTOM_OVERLAY
    val ctaDisplayTimingOrdinal: Int = 0, // AT_END_100
    val ctaTransparentBg: Boolean = false,
    val ctaShowBorder: Boolean = true,
    val ctaShowHandle: Boolean = true,
    val ctaAccentIndex: Int = 1, // CRIMSON_EMERALD
    val ctaSizeScaleIndex: Int = 1, // STANDARD
    val endCtaMessage: String = "Like, Comment & Subscribe for more!",
    val ctaShowLike: Boolean = true,
    val ctaShowComment: Boolean = true,
    val ctaShowSubscribe: Boolean = true,
    val ctaLikeLabel: String = "Like",
    val ctaCommentLabel: String = "Comment",
    val ctaSubscribeLabel: String = "SUBSCRIBE",
    val ctaSubscribedLabel: String = "SUBSCRIBED ✓",
    val isAutoChainTabs: Boolean = true,
    val autoRevealPreviewOnFinish: Boolean = false,
    val backdropPresetIndex: Int = 1,
    val showViralHookBanner: Boolean = true,
    val viralHookBadge: String = "🔥 HTML • CSS • JS",
    val viralHookTitle: String = "Build a Developer Portfolio Card",
    val viralHookStyleOrdinal: Int = 0,
    val viralHookPositionOrdinal: Int = 0, // TOP_STAGE
    val viralHookAlignmentOrdinal: Int = 0, // START
    val viralHookTransparentBg: Boolean = false,
    val viralHookShowBorder: Boolean = true,
    val viralHookShowBadge: Boolean = true,
    val viralHookAccentIndex: Int = 0, // CYBER_CYAN_PINK
    val viralHookSizeScaleIndex: Int = 1, // STANDARD
    val typingSoundProfileIndex: Int = 1, // MECH_THOCK
    val useRecordingCountdown: Boolean = true,
    val keepPreviewOpenInRecord: Boolean = true,
    val hasCompletedOnboarding: Boolean = false,
    val lastSavedTimestamp: Long = System.currentTimeMillis()
)

data class ProjectPreset(
    val id: String,
    val name: String,
    val description: String,
    val hookBadge: String,
    val hookTitle: String,
    val html: String,
    val css: String,
    val js: String
)

object ProjectTemplates {
    val PORTFOLIO_HTML = """
        <!-- Developer Portfolio Card -->
        <div class="card">
          <span class="badge">Available for Projects</span>
          <h1 id="title">Aarav Sharma</h1>
          <p id="bio">Android &amp; Full-Stack Developer building clean, fast mobile experiences.</p>

          <div class="stats">
            <span class="label">Completed Projects: 24</span>
            <progress value="80" max="100"></progress>
          </div>

          <div class="action-row">
            <button id="btn-hello" onclick="sayHello()">Say Hello</button>
            <button id="btn-work" onclick="showSkills()">View Skills</button>
          </div>
        </div>
    """.trimIndent()

    val PORTFOLIO_CSS = """
        /* Clean Modern Card Styles */
        body {
          background-color: #F8FAFC;
          color: #0F172A;
          padding: 16px;
        }

        .card {
          background-color: #FFFFFF;
          border-color: #CBD5E1;
          border-radius: 16px;
          padding: 16px;
        }

        .badge {
          background-color: #DBEAFE;
          color: #1D4ED8;
          padding: 6px;
          border-radius: 8px;
          font-size: 12px;
        }

        h1 {
          color: #0F172A;
          font-size: 20px;
          font-weight: bold;
        }

        p {
          color: #475569;
          font-size: 14px;
        }

        button {
          background-color: #2563EB;
          color: #FFFFFF;
          border-radius: 10px;
          padding: 10px;
          font-weight: bold;
        }
    """.trimIndent()

    val PORTFOLIO_JS = """
        // Portfolio Card Interactions
        let greetingCount = 0;

        console.info("Portfolio view initialized");

        function sayHello() {
          greetingCount = greetingCount + 1;
          document.getElementById("title").innerText = "Thanks for visiting!";
          document.getElementById("bio").innerText = "You sent a greeting (" + greetingCount + "x). Let's build something great.";
          console.log("Greeting sent! Total count: " + greetingCount);
        }

        function showSkills() {
          document.getElementById("bio").innerText = "Skills: Kotlin, Jetpack Compose, HTML5, CSS3, JavaScript & UI Design.";
          console.info("Displayed developer skills list.");
        }
    """.trimIndent()

    val CYBERPUNK_HTML = """
        <!-- Neon Cyberpunk 2077 Neural Pass -->
        <div class="card">
          <span class="badge">NETRUNNER LEVEL 99</span>
          <h1 id="cyber-title">NIGHT CITY ACCESS PASS</h1>
          <p id="cyber-status">Neural Link Encrypted • Bio-Monitor Online</p>

          <div class="stats">
            <span class="label">Cyberware Capacity: 94%</span>
            <progress value="94" max="100"></progress>
          </div>

          <div class="action-row">
            <button id="btn-breach" onclick="runBreachProtocol()">Breach Protocol</button>
            <button id="btn-overclock" onclick="activateOverclock()">Overclock GPU</button>
          </div>
        </div>
    """.trimIndent()

    val CYBERPUNK_CSS = """
        /* Neon Cyberpunk Dark Glass Theme */
        body {
          background-color: #070913;
          color: #F8FAFC;
          padding: 16px;
        }

        .card {
          background-color: #0F172A;
          border-color: #06B6D4;
          border-radius: 16px;
          padding: 16px;
        }

        .badge {
          background-color: #164E63;
          color: #22D3EE;
          padding: 6px;
          border-radius: 8px;
          font-size: 11px;
          font-weight: bold;
        }

        h1 {
          color: #F43F5E;
          font-size: 19px;
          font-weight: bold;
        }

        p {
          color: #94A3B8;
          font-size: 13px;
        }

        .stats {
          background-color: #1E293B;
          border-radius: 10px;
          padding: 10px;
        }

        button {
          background-color: #EC4899;
          color: #FFFFFF;
          border-radius: 10px;
          padding: 10px;
          font-weight: bold;
        }
    """.trimIndent()

    val CYBERPUNK_JS = """
        // Cyberpunk Neural Engine
        let daemonsUploaded = 3;
        console.info("Night City Neural Interface connected.");

        function runBreachProtocol() {
          daemonsUploaded = daemonsUploaded + 1;
          document.getElementById("cyber-title").innerText = "ICE BREACHED (" + daemonsUploaded + " DAEMONS)";
          document.getElementById("cyber-status").innerText = "Root access granted to Arasaka Subnet.";
          document.getElementById("cyber-title").style.color = "#22D3EE";
          console.log("Breach protocol succeeded: " + daemonsUploaded + " daemons active.");
        }

        function activateOverclock() {
          document.getElementById("cyber-title").innerText = "NEURAL OVERCLOCK 240%";
          document.getElementById("cyber-status").innerText = "Time dilation active. Zero latency mode engaged.";
          document.getElementById("cyber-title").style.color = "#F59E0B";
          console.warn("Thermal warning: Cyberdeck running at 240% clock speed.");
        }
    """.trimIndent()

    val POMODORO_HTML = """
        <!-- Deep Work Pomodoro Focus App -->
        <div class="card">
          <span class="badge">DEEP WORK SESSION</span>
          <h1 id="pomo-timer">25:00 FOCUS</h1>
          <p id="pomo-task">Current Sprint: Ship Web Dev Shorts Studio</p>
          <progress value="75" max="100"></progress>

          <div class="action-row">
            <button id="btn-sprint" onclick="completeSprint()">Complete Sprint</button>
            <button id="btn-break" onclick="takeShortBreak()">5m Break</button>
          </div>
        </div>
    """.trimIndent()

    val POMODORO_CSS = """
        body {
          background-color: #0F172A;
          color: #F8FAFC;
          padding: 16px;
        }

        .card {
          background-color: #1E293B;
          border-color: #8B5CF6;
          border-radius: 16px;
          padding: 16px;
        }

        .badge {
          background-color: #2E1065;
          color: #C4B5FD;
          padding: 6px;
          border-radius: 8px;
          font-size: 11px;
          font-weight: bold;
        }

        h1 {
          color: #F8FAFC;
          font-size: 21px;
          font-weight: bold;
        }

        p {
          color: #CBD5E1;
          font-size: 13px;
        }

        button {
          background-color: #7C3AED;
          color: #FFFFFF;
          border-radius: 10px;
          padding: 10px;
          font-weight: bold;
        }
    """.trimIndent()

    val POMODORO_JS = """
        // Pomodoro Controller
        let completedSprints = 4;
        console.log("Pomodoro timer ready. Sprints today: " + completedSprints);

        function completeSprint() {
          completedSprints = completedSprints + 1;
          document.getElementById("pomo-timer").innerText = "SPRINT #" + completedSprints + " DONE!";
          document.getElementById("pomo-task").innerText = "Streak boosted! Total focus: " + completedSprints + " sessions.";
          document.getElementById("pomo-timer").style.color = "#10B981";
          console.info("Completed sprint #" + completedSprints);
        }

        function takeShortBreak() {
          document.getElementById("pomo-timer").innerText = "05:00 RECHARGE";
          document.getElementById("pomo-task").innerText = "Hydrate & stretch before the next coding block.";
          document.getElementById("pomo-timer").style.color = "#38BDF8";
          console.log("Switched to 5-minute recovery break.");
        }
    """.trimIndent()

    val CRYPTO_HTML = """
        <!-- Live Web3 Crypto Pulse Card -->
        <div class="card">
          <span class="badge">LIVE MAINNET FEED</span>
          <h1 id="eth-price">ETH $3,420.00 (+6.8%)</h1>
          <p id="eth-gas">Gas: 12 Gwei • Block #19,482,910 Finalized</p>

          <div class="stats">
            <span class="label">24h Liquidity Pool Volume: 88%</span>
            <progress value="88" max="100"></progress>
          </div>

          <div class="action-row">
            <button id="btn-buy" onclick="executeSwap()">Instant Swap</button>
            <button id="btn-alert" onclick="triggerWhaleAlert()">Whale Alert</button>
          </div>
        </div>
    """.trimIndent()

    val CRYPTO_CSS = """
        body {
          background-color: #061311;
          color: #ECFDF5;
          padding: 16px;
        }

        .card {
          background-color: #062822;
          border-color: #10B981;
          border-radius: 16px;
          padding: 16px;
        }

        .badge {
          background-color: #064E3B;
          color: #34D399;
          padding: 6px;
          border-radius: 8px;
          font-size: 11px;
          font-weight: bold;
        }

        h1 {
          color: #34D399;
          font-size: 20px;
          font-weight: bold;
        }

        p {
          color: #A7F3D0;
          font-size: 13px;
        }

        .stats {
          background-color: #042F2E;
          border-radius: 10px;
          padding: 10px;
        }

        button {
          background-color: #059669;
          color: #FFFFFF;
          border-radius: 10px;
          padding: 10px;
          font-weight: bold;
        }
    """.trimIndent()

    val CRYPTO_JS = """
        // Web3 DEX Feed Simulator
        let swapCount = 1;
        console.info("Connected to Mainnet WebSocket feed.");

        function executeSwap() {
          swapCount = swapCount + 1;
          document.getElementById("eth-price").innerText = "SWAPPED 2.5 ETH (#" + swapCount + ")";
          document.getElementById("eth-gas").innerText = "Zero-slippage order settled in 1.2s.";
          console.log("DEX Swap #" + swapCount + " confirmed on-chain.");
        }

        function triggerWhaleAlert() {
          document.getElementById("eth-price").innerText = "WHALE BUY: +1,200 ETH!";
          document.getElementById("eth-gas").innerText = "Institutional wallet accumulated $4.1M ETH.";
          document.getElementById("eth-price").style.color = "#F59E0B";
          console.warn("Whale alert detected on Uniswap V3 pool!");
        }
    """.trimIndent()

    val COUNTER_HTML = """
        <!-- Daily Study Goal Tracker -->
        <div class="card">
          <span class="badge">Daily Tracker</span>
          <h1 id="counter-title">25 mins completed</h1>
          <p id="counter-status">Keep going! Consistent practice builds mastery.</p>
          <progress value="65" max="100"></progress>

          <div class="action-row">
            <button id="btn-add" onclick="addSession()">+15 Mins</button>
            <button id="btn-reset" onclick="resetTracker()">Reset</button>
          </div>
        </div>
    """.trimIndent()

    val COUNTER_CSS = """
        body {
          background-color: #F8FAFC;
          color: #0F172A;
          padding: 16px;
        }

        .card {
          background-color: #FFFFFF;
          border-color: #E2E8F0;
          border-radius: 16px;
          padding: 16px;
        }

        .badge {
          background-color: #DCFCE7;
          color: #15803D;
          padding: 6px;
          border-radius: 8px;
          font-size: 12px;
        }

        h1 {
          color: #1E293B;
          font-size: 20px;
          font-weight: bold;
        }

        button {
          background-color: #16A34A;
          color: #FFFFFF;
          border-radius: 10px;
          padding: 10px;
          font-weight: bold;
        }
    """.trimIndent()

    val COUNTER_JS = """
        // Study Tracker Script
        let minutes = 25;
        console.log("Tracker loaded with " + minutes + " minutes.");

        function addSession() {
          minutes = minutes + 15;
          document.getElementById("counter-title").innerText = minutes + " mins completed";
          document.getElementById("counter-status").innerText = "Awesome progress! Session logged.";
          console.info("Added 15 mins -> Total: " + minutes + " mins");
        }

        function resetTracker() {
          minutes = 0;
          document.getElementById("counter-title").innerText = "0 mins completed";
          document.getElementById("counter-status").innerText = "Timer reset for a new day.";
          console.warn("Tracker reset to 0 minutes.");
        }
    """.trimIndent()

    val SAAS_DASHBOARD_HTML = """
        <!-- Full Cloud SaaS Analytics Dashboard -->
        <div class="card">
          <span class="badge">LIVE TELEMETRY v4.2</span>
          <h1 id="dash-title">CloudScale Production Cluster</h1>
          <p id="dash-sub">Real-time multi-region throughput, latency &amp; auto-scaling monitor.</p>

          <div class="stats">
            <span class="label">Active Edge Nodes: 128 / 140</span>
            <progress value="92" max="100"></progress>
          </div>

          <div class="stats">
            <span class="label">Memory Pool Utilization: 64%</span>
            <progress value="64" max="100"></progress>
          </div>

          <div class="stats">
            <span class="label">API Gateway Cache Hit Ratio: 98%</span>
            <progress value="98" max="100"></progress>
          </div>

          <div class="action-row">
            <button id="btn-scale" onclick="scaleCluster()">Scale +16 Nodes</button>
            <button id="btn-purge" onclick="purgeEdgeCache()">Purge CDN Cache</button>
            <button id="btn-audit" onclick="runHealthAudit()">Run Security Audit</button>
          </div>
        </div>
    """.trimIndent()

    val SAAS_DASHBOARD_CSS = """
        /* CloudScale Dark Enterprise Dashboard Theme */
        body {
          background-color: #0B0F19;
          color: #F8FAFC;
          padding: 16px;
        }

        .card {
          background-color: #111827;
          border-color: #1E293B;
          border-radius: 16px;
          padding: 16px;
        }

        .badge {
          background-color: #064E3B;
          color: #34D399;
          padding: 6px;
          border-radius: 8px;
          font-size: 11px;
          font-weight: bold;
        }

        h1 {
          color: #F8FAFC;
          font-size: 20px;
          font-weight: bold;
        }

        p {
          color: #94A3B8;
          font-size: 13px;
        }

        .stats {
          background-color: #1E293B;
          border-radius: 10px;
          padding: 10px;
        }

        .label {
          color: #E2E8F0;
          font-size: 12px;
          font-weight: bold;
        }

        button {
          background-color: #3B82F6;
          color: #FFFFFF;
          border-radius: 10px;
          padding: 10px;
          font-weight: bold;
        }
    """.trimIndent()

    val SAAS_DASHBOARD_JS = """
        // CloudScale Cluster Controller
        let activeNodes = 128;
        let deployVersion = 42;

        console.info("CloudScale Telemetry Engine connected.");
        console.log("Active cluster nodes online: " + activeNodes);

        function scaleCluster() {
          activeNodes = activeNodes + 16;
          document.getElementById("dash-title").innerText = "Cluster Scaled: " + activeNodes + " Nodes";
          document.getElementById("dash-sub").innerText = "Auto-scaler provisioned 16 new edge instances across 3 regions.";
          console.info("Cluster scaled up to " + activeNodes + " nodes.");
        }

        function purgeEdgeCache() {
          deployVersion = deployVersion + 1;
          document.getElementById("dash-sub").innerText = "Global CDN edge cache purged (Snapshot #" + deployVersion + ").";
          console.warn("CDN cache invalidated across all edge POPs.");
        }

        function runHealthAudit() {
          document.getElementById("dash-title").innerText = "Zero-Trust Audit: 100% Passed";
          document.getElementById("dash-sub").innerText = "TLS 1.3 enforced, mTLS active, 0 vulnerabilities detected.";
          console.log("Security audit completed with 0 warnings.");
        }
    """.trimIndent()

    val PRESETS = listOf(
        ProjectPreset(
            id = "portfolio",
            name = "Portfolio Card",
            description = "Clean personal developer profile card with interactive buttons",
            hookBadge = "🔥 HTML • CSS • JS",
            hookTitle = "Build a Developer Portfolio Card",
            html = PORTFOLIO_HTML,
            css = PORTFOLIO_CSS,
            js = PORTFOLIO_JS
        ),
        ProjectPreset(
            id = "cyberpunk_card",
            name = "Neon Cyberpunk UI",
            description = "High-contrast Cyberpunk 2077 neural card with dynamic JS color shift",
            hookBadge = "⚡ 30s UI CHALLENGE",
            hookTitle = "Cyberpunk 2077 UI in HTML & CSS",
            html = CYBERPUNK_HTML,
            css = CYBERPUNK_CSS,
            js = CYBERPUNK_JS
        ),
        ProjectPreset(
            id = "pomodoro_timer",
            name = "Pomodoro Focus App",
            description = "Deep work sprint timer with interactive session counter & break mode",
            hookBadge = "⏱️ WEB DEV SHORTS",
            hookTitle = "Interactive Pomodoro App in 30s",
            html = POMODORO_HTML,
            css = POMODORO_CSS,
            js = POMODORO_JS
        ),
        ProjectPreset(
            id = "crypto_ticker",
            name = "Web3 Crypto Ticker",
            description = "Real-time emerald DEX swap & whale alert dashboard card",
            hookBadge = "🚀 FRONTEND MAGIC",
            hookTitle = "Build a Live Crypto Ticker Card",
            html = CRYPTO_HTML,
            css = CRYPTO_CSS,
            js = CRYPTO_JS
        ),
        ProjectPreset(
            id = "study_tracker",
            name = "Study Goal Tracker",
            description = "Simple progress tracker with session counter & console logs",
            hookBadge = "📈 DAY 1 / 100",
            hookTitle = "Daily Coding Goal Tracker UI",
            html = COUNTER_HTML,
            css = COUNTER_CSS,
            js = COUNTER_JS
        ),
        ProjectPreset(
            id = "saas_dashboard",
            name = "Cloud SaaS Dashboard (Large)",
            description = "Multi-section cloud telemetry dashboard for testing large code auto-write",
            hookBadge = "💻 FULL STACK UI",
            hookTitle = "Cloud Telemetry Dashboard in 60s",
            html = SAAS_DASHBOARD_HTML,
            css = SAAS_DASHBOARD_CSS,
            js = SAAS_DASHBOARD_JS
        )
    )
}

@Entity(tableName = "code_revisions")
data class CodeRevisionSnapshot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val tag: String = "Manual Snapshot",
    val htmlCode: String = "",
    val cssCode: String = "",
    val jsCode: String = "",
    val totalChars: Int = 0
)
