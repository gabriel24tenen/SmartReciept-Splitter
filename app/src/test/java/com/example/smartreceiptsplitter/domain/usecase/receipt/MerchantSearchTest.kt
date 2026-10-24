package com.example.smartreceiptsplitter.domain.usecase.receipt

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MerchantSearchTest {

    @Test
    fun blankQueryMatchesEverythingEvenAMissingName() {
        assertTrue(matchesMerchant("Konzum", ""))
        assertTrue(matchesMerchant("Konzum", "   "))
        assertTrue(matchesMerchant(null, ""))
    }

    @Test
    fun findsPartOfTheNameIgnoringCase() {
        assertTrue(matchesMerchant("Konzum d.o.o.", "konz"))
        assertTrue(matchesMerchant("konzum", "KONZUM"))
        assertTrue(matchesMerchant("Konzum d.o.o.", "zum d"))
    }

    @Test
    fun ignoresSpacesAroundTheQuery() {
        assertTrue(matchesMerchant("Konzum", "  konzum "))
    }

    @Test
    fun capitalCroatianLettersMatchSmallOnes() {
        // SQL LIKE would fail here, it only ignores case for ASCII letters
        assertTrue(matchesMerchant("Žabac", "žabac"))
        assertTrue(matchesMerchant("žabac", "ŽABAC"))
        assertTrue(matchesMerchant("Šoping centar", "šop"))
        assertTrue(matchesMerchant("ĆEVAPI", "ćevapi"))
    }

    @Test
    fun findsNamesWithDiacriticsWhenTheQueryHasNone() {
        assertTrue(matchesMerchant("Žabac", "zabac"))
        assertTrue(matchesMerchant("Ćevabdžinica", "cevab"))
        assertTrue(matchesMerchant("Čakovec Market", "cakovec"))
        assertTrue(matchesMerchant("Đurđevac", "durdevac"))
        assertTrue(matchesMerchant("Đurđevac", "ĐURĐ"))
    }

    @Test
    fun diacriticsInTheQueryFindPlainNames() {
        assertTrue(matchesMerchant("Zabac", "žabac"))
    }

    @Test
    fun doesNotMatchOtherNames() {
        assertFalse(matchesMerchant("Konzum", "Tommy"))
        assertFalse(matchesMerchant("Konzum", "konzumx"))
    }

    @Test
    fun receiptWithoutNameOnlyMatchesABlankQuery() {
        assertFalse(matchesMerchant(null, "konzum"))
        assertFalse(matchesMerchant("", "konzum"))
    }

    @Test
    fun specialCharactersAreSearchedLiterally() {
        // not wildcards like in LIKE
        assertFalse(matchesMerchant("Konzum", "%"))
        assertFalse(matchesMerchant("Konzum", "_onzum"))
        assertTrue(matchesMerchant("100% Sok", "100%"))
    }
}
