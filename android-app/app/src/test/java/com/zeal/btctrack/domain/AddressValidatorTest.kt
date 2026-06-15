package com.zeal.btctrack.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AddressValidatorTest {

    @Test
    fun `valid legacy P2PKH address passes`() {
        assertTrue(AddressValidator.isValid("1A1zP1eP5QGefi2DMPTfTL5SLmv7Divf"))
    }

    @Test
    fun `valid P2SH address passes`() {
        assertTrue(AddressValidator.isValid("3J98t1WpEZ73CNmQviecrnyiWrnqRhWNLy"))
    }

    @Test
    fun `valid native segwit bc1 address passes`() {
        assertTrue(AddressValidator.isValid("bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq"))
    }

    @Test
    fun `empty string fails`() {
        assertFalse(AddressValidator.isValid(""))
    }

    @Test
    fun `random text fails`() {
        assertFalse(AddressValidator.isValid("not-an-address"))
    }

    @Test
    fun `address with spaces fails`() {
        assertFalse(AddressValidator.isValid("bc1q ar0"))
    }
}
