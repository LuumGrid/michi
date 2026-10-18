package com.luum.michi.app.root.state

/**
 * Back-close layer of a [RootOverlay], in the order back closes them: sheets
 * first, then the surfaces drawn above the detail page, then the overlays below
 * it. One back closes a whole layer at once.
 *
 * The in-context search is not an overlay: it is part of the tab surface, so in
 * back order it sits between the detail page and the [BELOW_DETAIL] layer.
 */
internal enum class OverlayLayer {
    SHEET,
    ABOVE_DETAIL,
    BELOW_DETAIL,
}

/**
 * Every root overlay/sheet back can close. Membership is not exclusive within a
 * layer: several overlays of the same layer can be open at once (SignIn opens
 * from Settings), and one back closes the whole layer, as before. The editor is
 * deliberately not here — it carries data (`editorMediaId` + initial state) and
 * keeps its own fields; it counts as [OverlayLayer.ABOVE_DETAIL] in back order.
 */
internal enum class RootOverlay(val layer: OverlayLayer) {
    ExploreFilter(OverlayLayer.SHEET),
    SectionFilter(OverlayLayer.SHEET),
    ListSort(OverlayLayer.SHEET),
    CalendarFilter(OverlayLayer.SHEET),
    DetailMenu(OverlayLayer.SHEET),
    DetailSort(OverlayLayer.SHEET),
    ShareProfile(OverlayLayer.ABOVE_DETAIL),
    Settings(OverlayLayer.ABOVE_DETAIL),
    SignIn(OverlayLayer.ABOVE_DETAIL),
    Explore(OverlayLayer.BELOW_DETAIL),
    Calendar(OverlayLayer.BELOW_DETAIL),
    Notifications(OverlayLayer.BELOW_DETAIL),
}
