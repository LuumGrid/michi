package com.luum.michi.app.core.model

/**
 * Page size for every query that feeds a horizontal rail: it is both the
 * `perPage` of that query and the number of items a rail shows (3 card-rail
 * rows × 4 columns, the same shape `DashboardRailPageSize` gives the
 * dashboard). "View all" starts at page 1 (already fetched with the detail) and
 * pages with the same size, so rail payloads and pagination stay consistent
 * across features.
 */
internal const val RailPageSize = 12

/**
 * Page size for full grids and complete lists (the studio media grid), which
 * show many posters at once and scroll, unlike a rail: 30 keeps the request
 * count low (AniList allows 30 req/min) while the auto-pagination footer keeps
 * filling the grid. Rails stay on [RailPageSize].
 */
internal const val GridPageSize = 30
