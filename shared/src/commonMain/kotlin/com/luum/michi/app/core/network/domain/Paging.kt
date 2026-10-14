package com.luum.michi.app.core.network.domain

/**
 * Page size for every query that feeds a horizontal rail: it is both the
 * `perPage` of that query and the number of items a rail shows. "View all"
 * starts at page 1 (already fetched with the detail) and pages with the same
 * size, so rail payloads and pagination stay consistent across features.
 */
internal const val RailPageSize = 10
