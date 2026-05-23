package com.luum.michi.app.account.presentation.sample

import androidx.compose.ui.graphics.Color
import com.luum.michi.app.account.presentation.model.AccountFavoriteMedia
import com.luum.michi.app.account.presentation.model.AccountFavoritePerson
import com.luum.michi.app.account.presentation.model.AccountFavoriteStudio
import com.luum.michi.app.account.presentation.model.AccountFavorites

internal val AccountSampleFavorites: AccountFavorites = AccountFavorites(
    anime = listOf(
        AccountFavoriteMedia(1, "Frieren: Beyond Journey's End", null, listOf(Color(0xFF0F766E), Color(0xFF99F6E4))),
        AccountFavoriteMedia(2, "Vinland Saga", null, listOf(Color(0xFF1E3A8A), Color(0xFF93C5FD))),
        AccountFavoriteMedia(3, "Mushishi", null, listOf(Color(0xFF064E3B), Color(0xFFA7F3D0))),
        AccountFavoriteMedia(4, "Mob Psycho 100", null, listOf(Color(0xFF7C2D12), Color(0xFFFDBA74))),
        AccountFavoriteMedia(5, "Steins;Gate", null, listOf(Color(0xFF312E81), Color(0xFFA5B4FC))),
        AccountFavoriteMedia(6, "Monogatari", null, listOf(Color(0xFF831843), Color(0xFFF9A8D4))),
    ),
    manga = listOf(
        AccountFavoriteMedia(101, "Berserk", null, listOf(Color(0xFF1E293B), Color(0xFF64748B))),
        AccountFavoriteMedia(102, "Vagabond", null, listOf(Color(0xFF064E3B), Color(0xFF059669))),
        AccountFavoriteMedia(103, "Monster", null, listOf(Color(0xFF450A0A), Color(0xFF991B1B))),
        AccountFavoriteMedia(104, "20th Century Boys", null, listOf(Color(0xFF1F2937), Color(0xFFFBBF24))),
        AccountFavoriteMedia(105, "Oyasumi Punpun", null, listOf(Color(0xFF3F3F46), Color(0xFFA1A1AA))),
        AccountFavoriteMedia(106, "Chainsaw Man", null, listOf(Color(0xFF7C2D12), Color(0xFFEA580C))),
    ),
    characters = listOf(
        AccountFavoritePerson(201, "Frieren", null, listOf(Color(0xFF0F766E), Color(0xFFA7F3D0))),
        AccountFavoritePerson(202, "Lelouch Lamperouge", null, listOf(Color(0xFF581C87), Color(0xFFD8B4FE))),
        AccountFavoritePerson(203, "Guts", null, listOf(Color(0xFF18181B), Color(0xFFEF4444))),
        AccountFavoritePerson(204, "Thorfinn", null, listOf(Color(0xFF075985), Color(0xFFBAE6FD))),
        AccountFavoritePerson(205, "Okabe Rintaro", null, listOf(Color(0xFF7C2D12), Color(0xFFFCA5A5))),
        AccountFavoritePerson(206, "Ginko", null, listOf(Color(0xFF14532D), Color(0xFFA7F3D0))),
    ),
    staff = listOf(
        AccountFavoritePerson(301, "Hayao Miyazaki", null, listOf(Color(0xFF1E40AF), Color(0xFFBFDBFE))),
        AccountFavoritePerson(302, "Satoshi Kon", null, listOf(Color(0xFF7C3AED), Color(0xFFC4B5FD))),
        AccountFavoritePerson(303, "Kentaro Miura", null, listOf(Color(0xFF1F2937), Color(0xFFA1A1AA))),
        AccountFavoritePerson(304, "Naoki Urasawa", null, listOf(Color(0xFF0F172A), Color(0xFF38BDF8))),
        AccountFavoritePerson(305, "Makoto Shinkai", null, listOf(Color(0xFF075985), Color(0xFF7DD3FC))),
        AccountFavoritePerson(306, "Hiromu Arakawa", null, listOf(Color(0xFF854D0E), Color(0xFFFDE68A))),
    ),
    studios = listOf(
        AccountFavoriteStudio(401, "MAPPA", listOf(Color(0xFF0F172A), Color(0xFF1E3A8A))),
        AccountFavoriteStudio(402, "Madhouse", listOf(Color(0xFF7C2D12), Color(0xFFEA580C))),
        AccountFavoriteStudio(403, "Bones", listOf(Color(0xFF14532D), Color(0xFF22C55E))),
        AccountFavoriteStudio(404, "Studio Ghibli", listOf(Color(0xFF1E40AF), Color(0xFF60A5FA))),
        AccountFavoriteStudio(405, "Wit Studio", listOf(Color(0xFF581C87), Color(0xFFC084FC))),
        AccountFavoriteStudio(406, "Trigger", listOf(Color(0xFF991B1B), Color(0xFFFCA5A5))),
    ),
)
