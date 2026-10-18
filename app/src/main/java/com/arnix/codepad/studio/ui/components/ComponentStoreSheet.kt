package com.arnix.codepad.studio.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arnix.codepad.studio.data.CodeTab
import com.arnix.codepad.studio.ui.theme.JetBrainsMonoFontFamily
import com.arnix.codepad.studio.ui.theme.uiSp

data class CodeSnippet(
    val id: String,
    val title: String,
    val category: String,
    val description: String,
    val targetTab: CodeTab,
    val codeSnippet: String,
    val fullPresetHtml: String? = null,
    val fullPresetCss: String? = null,
    val fullPresetJs: String? = null
)

object SnippetCatalog {
    val ALL_SNIPPETS = listOf(
        CodeSnippet(
            id = "neon_glitch_btn",
            title = "Cyberpunk Neon Button",
            category = "UI Components",
            description = "High-voltage glowing neon button with pulse animation & haptic click ripple",
            targetTab = CodeTab.STYLE_CSS,
            codeSnippet = """
                .neon-btn {
                  background: #090D16;
                  color: #00F0FF;
                  border: 2px solid #00F0FF;
                  border-radius: 8px;
                  padding: 12px 24px;
                  font-family: monospace;
                  font-weight: bold;
                  text-transform: uppercase;
                  box-shadow: 0 0 15px rgba(0, 240, 255, 0.45);
                  cursor: pointer;
                  transition: all 0.2s ease;
                }
                .neon-btn:hover {
                  background: #00F0FF;
                  color: #050811;
                  box-shadow: 0 0 25px rgba(0, 240, 255, 0.85);
                }
            """.trimIndent()
        ),
        CodeSnippet(
            id = "glass_metric_card",
            title = "Glassmorphism Metric Card",
            category = "UI Components",
            description = "Frosted glass container with backdrop blur and vibrant gradient rim",
            targetTab = CodeTab.STYLE_CSS,
            codeSnippet = """
                .glass-card {
                  background: rgba(255, 255, 255, 0.08);
                  backdrop-filter: blur(16px);
                  -webkit-backdrop-filter: blur(16px);
                  border: 1px solid rgba(255, 255, 255, 0.18);
                  border-radius: 16px;
                  padding: 20px;
                  box-shadow: 0 8px 32px 0 rgba(0, 0, 0, 0.37);
                  color: #F8FAFC;
                }
            """.trimIndent()
        ),
        CodeSnippet(
            id = "canvas_particles",
            title = "Interactive Fluid Particles",
            category = "Interactive JS",
            description = "60fps interactive HTML5 canvas particle matrix with cursor connections",
            targetTab = CodeTab.SCRIPT_JS,
            codeSnippet = """
                // Lightweight Interactive Particle Matrix
                const canvas = document.createElement('canvas');
                canvas.id = 'particle-bg';
                document.body.prepend(canvas);
                const ctx = canvas.getContext('2d');
                canvas.width = window.innerWidth;
                canvas.height = window.innerHeight;

                const particles = Array.from({ length: 45 }, () => ({
                  x: Math.random() * canvas.width,
                  y: Math.random() * canvas.height,
                  vx: (Math.random() - 0.5) * 1.5,
                  vy: (Math.random() - 0.5) * 1.5,
                  size: Math.random() * 2.5 + 1
                }));

                function animate() {
                  ctx.clearRect(0, 0, canvas.width, canvas.height);
                  ctx.fillStyle = 'rgba(56, 189, 248, 0.8)';
                  particles.forEach(p => {
                    p.x += p.vx;
                    p.y += p.vy;
                    if (p.x < 0 || p.x > canvas.width) p.vx *= -1;
                    if (p.y < 0 || p.y > canvas.height) p.vy *= -1;
                    ctx.beginPath();
                    ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2);
                    ctx.fill();
                  });
                  requestAnimationFrame(animate);
                }
                animate();
            """.trimIndent()
        ),
        CodeSnippet(
            id = "responsive_navbar",
            title = "Modern Responsive Navbar",
            category = "Layouts",
            description = "Sticky header with brand logo, nav links and pill action button",
            targetTab = CodeTab.INDEX_HTML,
            codeSnippet = """
                <nav class="studio-nav">
                  <div class="logo">⚡ CodePad</div>
                  <ul class="nav-links">
                    <li><a href="#projects">Projects</a></li>
                    <li><a href="#stack">Stack</a></li>
                    <li><a href="#contact" class="cta-link">Get in Touch</a></li>
                  </ul>
                </nav>
            """.trimIndent()
        ),
        CodeSnippet(
            id = "confetti_trigger",
            title = "Confetti Fireworks Blast",
            category = "Interactive JS",
            description = "Celebratory particle burst trigger using pure JavaScript canvas",
            targetTab = CodeTab.SCRIPT_JS,
            codeSnippet = """
                function launchConfetti(originX, originY) {
                  const colors = ['#38BDF8', '#F43F5E', '#10B981', '#F59E0B', '#8B5CF6'];
                  const c = document.createElement('canvas');
                  c.style.cssText = 'position:fixed;top:0;left:0;width:100%;height:100%;pointer-events:none;z-index:9999;';
                  document.body.appendChild(c);
                  const ctx = c.getContext('2d');
                  c.width = window.innerWidth;
                  c.height = window.innerHeight;

                  let bits = Array.from({ length: 70 }, () => ({
                    x: originX || c.width / 2,
                    y: originY || c.height / 2,
                    vx: (Math.random() - 0.5) * 12,
                    vy: (Math.random() - 0.8) * 12,
                    color: colors[Math.floor(Math.random() * colors.length)],
                    life: 1
                  }));

                  function loop() {
                    ctx.clearRect(0, 0, c.width, c.height);
                    bits.forEach(b => {
                      b.x += b.vx;
                      b.y += b.vy;
                      b.vy += 0.3; // gravity
                      b.life -= 0.02;
                      ctx.fillStyle = b.color;
                      ctx.fillRect(b.x, b.y, 6, 6);
                    });
                    bits = bits.filter(b => b.life > 0);
                    if (bits.length > 0) requestAnimationFrame(loop);
                    else document.body.removeChild(c);
                  }
                  loop();
                }
            """.trimIndent()
        ),
        CodeSnippet(
            id = "crypto_live_card",
            title = "Web3 DEX Swap Card",
            category = "Layouts",
            description = "Emerald decentralized exchange live card with swap button & telemetry",
            targetTab = CodeTab.INDEX_HTML,
            codeSnippet = """
                <div class="dex-card">
                  <div class="dex-header">
                    <span>ETH / USD</span>
                    <span class="live-tag">LIVE FEED</span>
                  </div>
                  <h2>$3,420.50</h2>
                  <p>24h High: $3,480.00 • Slippage: 0.1%</p>
                  <button class="swap-action" onclick="executeSwap()">Instant Swap</button>
                </div>
            """.trimIndent()
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComponentStoreSheet(
    onInsertSnippet: (CodeSnippet) -> Unit,
    onDismiss: () -> Unit
) {
    val categories = listOf("All", "UI Components", "Interactive JS", "Layouts")
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }

    val filteredSnippets = remember(selectedCategoryIndex) {
        if (selectedCategoryIndex == 0) SnippetCatalog.ALL_SNIPPETS
        else SnippetCatalog.ALL_SNIPPETS.filter { it.category == categories[selectedCategoryIndex] }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Widgets,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Component & Snippet Store",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "Insert pre-built production-grade UI components and scripts directly into your code tabs.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            // Category Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedCategoryIndex,
                edgePadding = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                categories.forEachIndexed { index, cat ->
                    Tab(
                        selected = selectedCategoryIndex == index,
                        onClick = { selectedCategoryIndex = index },
                        text = { Text(cat, fontSize = 12.uiSp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Snippet List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                items(filteredSnippets, key = { it.id }) { snippet ->
                    OutlinedCard(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = snippet.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = snippet.targetTab.fileName,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.uiSp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Text(
                                text = snippet.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Code preview snippet box
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = snippet.codeSnippet.lines().take(4).joinToString("\n") + if (snippet.codeSnippet.lines().size > 4) "\n..." else "",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.uiSp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(
                                    onClick = {
                                        onInsertSnippet(snippet)
                                        onDismiss()
                                    },
                                    modifier = Modifier.testTag("insert_snippet_${snippet.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Insert into ${snippet.targetTab.languageTag}", fontSize = 11.uiSp)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
