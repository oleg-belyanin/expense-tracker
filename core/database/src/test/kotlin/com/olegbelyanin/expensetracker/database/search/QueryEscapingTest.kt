package com.olegbelyanin.expensetracker.database.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QueryEscapingTest {
    @Test
    fun likeEscapesWildcards() {
        assertEquals("мага\\%зин%", LikeQuery.prefix("мага%зин"))
        assertEquals("foo\\_bar%", LikeQuery.prefix("foo_bar"))
        assertEquals("a\\\\b%", LikeQuery.prefix("a\\b"))
    }

    @Test
    fun ftsBuildsQuotedPrefixTokens() {
        assertEquals("\"мага\"* \"у\"*", FtsQuery.prefixMatch("мага у"))
        assertEquals("\"столичка\"*", FtsQuery.prefixMatch("столичка"))
        assertNull(FtsQuery.prefixMatch("   "))
        assertEquals("\"quote\"* \"d\"*", FtsQuery.prefixMatch("quote\"d"))
    }
}
