package com.github.prudhvir3ddy.jbinpb

import com.intellij.ui.JBColor
import com.intellij.ui.NewUI
import com.intellij.ui.scale.JBUIScale.scale
import com.intellij.util.ui.GraphicsUtil
import com.intellij.util.ui.JBInsets
import java.awt.AlphaComposite
import java.awt.BasicStroke
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.geom.Path2D
import java.awt.geom.RoundRectangle2D
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.plaf.basic.BasicProgressBarUI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Indian flag-themed progress bar with Rama icon and Ashoka Chakra.
 * Displays the Indian tricolor flag as the progress background with Lord Rama moving across it.
 */
@Suppress("MagicNumber", "TooManyFunctions")
internal class IndianProgressBarUI : BasicProgressBarUI() {

    private val flagColors = FlagColors(
        saffron = JBColor(FLAG_SAFFRON_LIGHT, FLAG_SAFFRON_DARK),
        white = JBColor(FLAG_WHITE_LIGHT, FLAG_WHITE_DARK),
        green = JBColor(FLAG_GREEN_LIGHT, FLAG_GREEN_DARK)
    )

    private val borderColor: JBColor
        get() = if (NewUI.isEnabled()) NEW_UI_BORDER_COLOR else OLD_UI_BORDER_COLOR

    // Animation state
    private var iconOffsetX = 0
    private var isGoingRight = false

    private var icon: Icon = RamaIcons.Rama

    private var iconSize = icon.iconHeight
        get() = icon.iconHeight
        set(value) {
            if (field != value) {
                field = value
                updateIcon()
            }
        }

    private var isIndeterminate: Boolean = false
        set(value) {
            if (field != value) resetState()
            field = value
        }

    private val resizeListener = object : ComponentAdapter() {
        override fun componentResized(e: ComponentEvent?) {
            updateIcon()
            progressBar.invalidate()
        }
    }

    private val borderInsets = JBInsets.create(BORDER_SIZE, BORDER_SIZE)

    init {
        updateIcon()
    }

    private fun updateIcon() {
        val maxSize = progressBar?.let { 
            it.height - it.insets.vertical - borderInsets.vertical 
        } ?: Int.MAX_VALUE

        val sizePx = iconSize.coerceAtMost(maxSize)
        icon = RamaIcons.Rama.resize(sizePx)
    }

    override fun installListeners() {
        super.installListeners()
        progressBar.addComponentListener(resizeListener)
    }

    override fun uninstallListeners() {
        progressBar.removeComponentListener(resizeListener)
        super.uninstallListeners()
    }

    override fun getPreferredSize(c: JComponent): Dimension {
        val verticalInsets = c.insets.vertical
        val borderThickness = borderInsets.top
        return Dimension(
            super.getPreferredSize(c).width,
            getPreferredStripeHeight() + verticalInsets + borderThickness
        )
    }

    private fun getPreferredStripeHeight(): Int {
        val ho = progressBar.getClientProperty("ProgressBar.stripeWidth")
        return ho?.toString()?.toIntOrNull()?.let { scale(it.coerceAtLeast(12)) }
            ?: defaultIconSize
    }

    override fun paintIndeterminate(g2d: Graphics, c: JComponent) {
        isIndeterminate = true
        if (g2d !is Graphics2D) return

        GraphicsUtil.setupAAPainting(g2d)

        val paintingContext = createPaintingContext()
        if (!paintingContext.isValid) return

        doAnimationTick(paintingContext.barWidth.toInt())

        with(g2d) {
            drawIndianFlag(
                x = paintingContext.contentX,
                y = paintingContext.contentY,
                barWidth = paintingContext.barWidth,
                barHeight = paintingContext.barHeight
            )

            drawBorder(
                x = paintingContext.x,
                y = paintingContext.y,
                boundsWidth = paintingContext.availableWidth - paintingContext.borderThickness,
                boundsHeight = paintingContext.availableHeight - paintingContext.borderThickness,
                borderThickness = paintingContext.borderThickness
            )

            if (shouldShowRamaIcon(paintingContext.availableHeight, paintingContext.barWidth)) {
                drawRamaIcon(
                    barY = paintingContext.contentY,
                    barHeight = paintingContext.barHeight,
                    xOffset = paintingContext.x + iconOffsetX.toFloat()
                )
            }
        }
    }

    override fun paintDeterminate(g2d: Graphics, c: JComponent) {
        isIndeterminate = false
        if (g2d !is Graphics2D) return

        GraphicsUtil.setupAAPainting(g2d)

        val paintingContext = createPaintingContext()
        if (!paintingContext.isValid) return

        val progress = calculateProgress()
        val progressBarWidth = calculateProgressBarWidth(paintingContext, progress)

        doAnimationTick(progressBarWidth.toInt())

        with(g2d) {
            drawIndianFlag(
                x = paintingContext.contentX,
                y = paintingContext.contentY,
                barWidth = (progressBarWidth - iconSize / 2f).coerceIn(
                    0f..(paintingContext.availableWidth - 2 * paintingContext.borderThickness)
                ),
                barHeight = paintingContext.barHeight
            )

            drawBorder(
                x = paintingContext.x + paintingContext.borderThickness / 2f,
                y = paintingContext.y + paintingContext.borderThickness / 2f,
                boundsWidth = paintingContext.availableWidth - paintingContext.borderThickness,
                boundsHeight = paintingContext.availableHeight - paintingContext.borderThickness,
                borderThickness = paintingContext.borderThickness
            )

            if (shouldShowRamaIcon(paintingContext.availableHeight, progressBarWidth)) {
                drawRamaIcon(
                    barY = paintingContext.contentY,
                    barHeight = paintingContext.barHeight,
                    xOffset = paintingContext.x + progressBarWidth - iconSize / 2f
                )
            }
        }
    }

    /**
     * Updates animation state for the moving Rama icon.
     */
    private fun doAnimationTick(barWidth: Int) {
        val animationSpeed = scale(ANIMATION_SPEED)
        iconOffsetX += if (isGoingRight) animationSpeed else -animationSpeed

        updateAnimationDirection(barWidth)
    }

    private fun updateAnimationDirection(barWidth: Int) {
        when {
            !isIndeterminate -> isGoingRight = true
            iconOffsetX > barWidth + iconSize / 2 -> isGoingRight = false
            iconOffsetX < -iconSize -> isGoingRight = true
        }
    }

    /**
     * Draws the Indian flag with three horizontal stripes and Ashoka Chakra.
     */
    private fun Graphics2D.drawIndianFlag(
        x: Float,
        y: Float,
        barWidth: Float,
        barHeight: Float
    ) {
        if (barWidth < MIN_DRAWABLE_WIDTH) return

        val barArc = scale(ARC_SIZE) / 2f
        val stripeHeight = barHeight / STRIPE_COUNT

        // Draw saffron stripe (top)
        drawFlagStripe(
            x, y, barWidth, stripeHeight,
            flagColors.saffron,
            hasTopRounding = true,
            hasBottomRounding = false,
            barArc
        )

        // Draw white stripe (middle)
        drawFlagStripe(
            x, y + stripeHeight, barWidth, stripeHeight,
            flagColors.white,
            hasTopRounding = false,
            hasBottomRounding = false,
            barArc
        )

        // Draw green stripe (bottom)
        drawFlagStripe(
            x, y + stripeHeight * 2, barWidth, stripeHeight,
            flagColors.green,
            hasTopRounding = false,
            hasBottomRounding = true,
            barArc
        )

        // Draw Ashoka Chakra in the center
        drawAshokaChakraIfSpaceAvailable(x, y, barWidth, barHeight)
    }

    /**
     * Draws the Ashoka Chakra with appropriate detail level based on size.
     */
    private fun Graphics2D.drawAshokaChakra(centerX: Float, centerY: Float, radius: Float) {
        color = CHAKRA_COLOR
        
        // Draw outer circle
        val outerStroke = BasicStroke((radius / 10f).coerceIn(MIN_STROKE_WIDTH, MAX_STROKE_WIDTH))
        stroke = outerStroke
        drawCircle(centerX, centerY, radius)

        // Draw spokes - fewer for small chakras
        val spokeCount = getSpokeCount(radius)
        val spokesStroke = BasicStroke((radius / 12f).coerceIn(MIN_STROKE_WIDTH * 0.6f, MAX_STROKE_WIDTH * 0.75f))
        stroke = spokesStroke
        
        repeat(spokeCount) { i ->
            drawChakraSpoke(centerX, centerY, radius, i, spokeCount)
        }

        // Draw center hub
        val centerRadius = (radius / 4f).coerceAtLeast(MIN_CENTER_RADIUS)
        drawCircle(centerX, centerY, centerRadius, filled = true)
    }

    private fun Graphics2D.drawBorder(
        x: Float,
        y: Float,
        boundsWidth: Float,
        boundsHeight: Float,
        borderThickness: Float
    ) {
        val borderArc = scale(ARC_SIZE)
        val borderShape = RoundRectangle2D.Float(x, y, boundsWidth, boundsHeight, borderArc, borderArc)
        
        stroke = BasicStroke(borderThickness)
        color = borderColor
        draw(borderShape)
    }


    /**
     * Draws the Rama icon with divine glow and shine effects.
     */
    private fun Graphics2D.drawRamaIcon(barY: Float, barHeight: Float, xOffset: Float) {
        val iconPosition = calculateIconPosition(barY, barHeight, xOffset)
        
        val originalComposite = composite as AlphaComposite
        
        // Draw divine glow
        drawDivineGlow(iconPosition.x, iconPosition.y, originalComposite)
        
        // Draw icon with high quality rendering
        enableHighQualityRendering()
        icon.paintIcon(progressBar, this, iconPosition.x, iconPosition.y)
        
        // Add shine effect
        drawShineEffect(iconPosition.x, iconPosition.y, originalComposite)
        
        composite = originalComposite
    }

    override fun getBoxLength(availableLength: Int, otherDimension: Int): Int = availableLength

    private fun resetState() {
        iconOffsetX = 0
        isGoingRight = true
    }

    // Helper methods for better code organization
    
    private fun createPaintingContext(): PaintingContext {
        val insets = progressBar.insets
        val availableWidth = (progressBar.width - insets.horizontal).toFloat()
        val availableHeight = (progressBar.height - insets.vertical).toFloat()
        val borderThickness = borderInsets.top.toFloat()
        
        return PaintingContext(
            x = insets.top.toFloat(),
            y = insets.left.toFloat(),
            availableWidth = availableWidth,
            availableHeight = availableHeight,
            borderThickness = borderThickness,
            barWidth = availableWidth - 2 * borderThickness,
            barHeight = availableHeight - 2 * borderThickness
        )
    }
    
    private fun calculateProgress(): Float = 
        progressBar.value.toFloat() / progressBar.maximum.coerceAtLeast(1)
    
    private fun calculateProgressBarWidth(context: PaintingContext, progress: Float): Float =
        (context.availableWidth - 2 * context.borderThickness) * progress + iconSize * progress
    
    private fun shouldShowRamaIcon(availableHeight: Float, barWidth: Float): Boolean =
        availableHeight >= MIN_ICON_DISPLAY_HEIGHT && barWidth >= MIN_ICON_DISPLAY_WIDTH

    private fun Graphics2D.drawAshokaChakraIfSpaceAvailable(x: Float, y: Float, barWidth: Float, barHeight: Float) {
        if (barWidth > CHAKRA_MIN_WIDTH && barHeight > CHAKRA_MIN_HEIGHT) {
            val chakraRadius = (barHeight / 6f).coerceIn(2f, 16f)
            drawAshokaChakra(x + barWidth / 2f, y + barHeight / 2f, chakraRadius)
        }
    }

    private fun Graphics2D.drawFlagStripe(
        x: Float, y: Float, width: Float, height: Float,
        color: JBColor, hasTopRounding: Boolean, hasBottomRounding: Boolean, arc: Float
    ) {
        val stripePath = Path2D.Float().apply {
            if (hasTopRounding) {
                moveTo(x, y + arc)
                quadTo(x, y, x + arc, y)
                lineTo(x + width - arc, y)
                quadTo(x + width, y, x + width, y + arc)
            } else {
                moveTo(x, y)
                lineTo(x + width, y)
            }
            
            lineTo(x + width, y + height - if (hasBottomRounding) arc else 0f)
            
            if (hasBottomRounding) {
                quadTo(x + width, y + height, x + width - arc, y + height)
                lineTo(x + arc, y + height)
                quadTo(x, y + height, x, y + height - arc)
            } else {
                lineTo(x + width, y + height)
                lineTo(x, y + height)
            }
            
            lineTo(x, y + if (hasTopRounding) arc else 0f)
            closePath()
        }
        paint = color
        fill(stripePath)
    }

    private fun getSpokeCount(radius: Float): Int = when {
        radius < 4f -> 8
        radius < 6f -> 12
        else -> 24
    }

    private fun Graphics2D.drawCircle(centerX: Float, centerY: Float, radius: Float, filled: Boolean = false) {
        val diameter = (radius * 2).toInt()
        val x = (centerX - radius).toInt()
        val y = (centerY - radius).toInt()
        
        if (filled) {
            fillOval(x, y, diameter, diameter)
        } else {
            drawOval(x, y, diameter, diameter)
        }
    }

    private fun Graphics2D.drawChakraSpoke(centerX: Float, centerY: Float, radius: Float, spokeIndex: Int, totalSpokes: Int) {
        val angle = Math.toRadians((spokeIndex * 360.0 / totalSpokes))
        val innerRadius = radius * 0.2f
        val outerRadius = radius * 0.85f
        
        val x1 = centerX + (innerRadius * cos(angle)).toFloat()
        val y1 = centerY + (innerRadius * sin(angle)).toFloat()
        val x2 = centerX + (outerRadius * cos(angle)).toFloat()
        val y2 = centerY + (outerRadius * sin(angle)).toFloat()
        
        drawLine(x1.toInt(), y1.toInt(), x2.toInt(), y2.toInt())
    }

    private fun calculateIconPosition(barY: Float, barHeight: Float, xOffset: Float): IconPosition {
        val yOffset = barY + barHeight / 2f - icon.iconHeight / 2f
        
        return IconPosition(
            (xOffset - iconSize / 2f).roundToInt(),
            yOffset.roundToInt()
        )
    }

    private fun Graphics2D.drawDivineGlow(x: Int, y: Int, originalComposite: AlphaComposite) {
        composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f)
        val glowSize = (iconSize * 1.4f).toInt()
        paint = JBColor(0xFFD700, 0xB8860B) // Golden glow
        fillOval(
            x - (glowSize - iconSize) / 2,
            y - (glowSize - iconSize) / 2,
            glowSize,
            glowSize
        )
        composite = originalComposite
    }

    private fun Graphics2D.drawShineEffect(x: Int, y: Int, originalComposite: AlphaComposite) {
        composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2f)
        paint = JBColor(0xFFFFFF, 0xFFFFFF) // White highlight
        val highlightSize = iconSize / 4
        fillOval(
            x + iconSize / 4,
            y + iconSize / 6,
            highlightSize,
            highlightSize / 2
        )
        composite = originalComposite
    }

    private fun Graphics2D.enableHighQualityRendering() {
        setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    }

    // Data classes for better type safety and organization
    private data class FlagColors(
        val saffron: JBColor,
        val white: JBColor,
        val green: JBColor
    )

    private data class IconPosition(val x: Int, val y: Int)
    
    private data class PaintingContext(
        val x: Float,
        val y: Float,
        val availableWidth: Float,
        val availableHeight: Float,
        val borderThickness: Float,
        val barWidth: Float,
        val barHeight: Float
    ) {
        val contentX: Float = x + borderThickness
        val contentY: Float = y + borderThickness
        val isValid: Boolean = availableWidth > 0 && availableHeight > 0
    }

    companion object {
        // Indian flag official colors
        private const val FLAG_SAFFRON_LIGHT = 0xFF9933
        private const val FLAG_WHITE_LIGHT = 0xFFFFFF
        private const val FLAG_GREEN_LIGHT = 0x138808

        private const val FLAG_SAFFRON_DARK = 0xCC7722
        private const val FLAG_WHITE_DARK = 0xDDDDDD
        private const val FLAG_GREEN_DARK = 0x0F6606

        // UI constants
        private val OLD_UI_BORDER_COLOR = JBColor(0xCCCCCC, 0x555555)
        private val NEW_UI_BORDER_COLOR = JBColor(0xCCCCCC, 0x111111)
        private val CHAKRA_COLOR = JBColor(0x000080, 0x000080)

        // Layout constants
        private const val STRIPE_COUNT = 3f
        private const val ARC_SIZE = 8f
        private const val BORDER_SIZE = 1
        private const val ANIMATION_SPEED = 2
        private const val MIN_DRAWABLE_WIDTH = 1f
        private const val MIN_ICON_DISPLAY_HEIGHT = 12f
        private const val MIN_ICON_DISPLAY_WIDTH = 16f
        
        // Chakra constants
        private const val MIN_STROKE_WIDTH = 0.5f
        private const val MAX_STROKE_WIDTH = 2f
        private const val MIN_CENTER_RADIUS = 1f
        private const val CHAKRA_MIN_WIDTH = 8f
        private const val CHAKRA_MIN_HEIGHT = 6f

        private val defaultIconSize: Int
            get() = scale(20)
    }
}