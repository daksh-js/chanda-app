package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ChandaRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createDonorAndRecordCashDonation_authenticatedOwner_succeedsAndEmitsUpdates() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = ChandaRepository(firestore)

        val createDonorResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createDonor(
                donorCode = "DONOR-0001",
                fullName = "Choudhary Surender Sangwan",
                fatherName = "Choudhary Ramphal Sangwan",
                mobileNumber = "9812345678",
                whatsappNumber = "9812345678",
                address = "Main Chopal Road",
                village = "Chhara",
                city = "Jhajjar",
                district = "Jhajjar",
                state = "Haryana",
                pinCode = "124504",
                addedByAdmin = "Vikramaditya Sengupta"
            )
        }
        assertTrue("Expected donor creation to succeed: ${createDonorResult.exceptionOrNull()}", createDonorResult.isSuccess)
        val donorId = createDonorResult.getOrThrow()

        val fetchedDonor = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getDonorById(donorId).getOrThrow()
        }
        assertNotNull(fetchedDonor)

        val receiptResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.recordCashDonation(
                donor = fetchedDonor!!,
                amount = 5100L,
                donationDate = "2026-11-04",
                purpose = "Kushti Peeth Diet Grant",
                receivedByAdmin = "Vikramaditya Sengupta",
                akhadaName = "Shri Bajrang Kushti Akhada Trust",
                akhadaAddress = "Chhara, Jhajjar, Haryana",
                thankYouMessage = "Dhanyawad"
            )
        }
        assertTrue("Expected cash donation to succeed: ${receiptResult.exceptionOrNull()}", receiptResult.isSuccess)
        assertEquals("CASH", receiptResult.getOrThrow().paymentMethod)

        val emittedDonors = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeDonors(uid).first { list -> list.any { it.id == donorId && it.lifetimeDonation == 5100L } }
        }
        assertTrue(emittedDonors.any { it.id == donorId })
    }

    @Test
    fun getDonorById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val aliceRepo = ChandaRepository(firestore)
        val donorId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createDonor(
                donorCode = "DONOR-0002",
                fullName = "Raghunath Sharma",
                fatherName = "Pt. Devdutt Sharma",
                mobileNumber = "9876543210",
                whatsappNumber = "9876543210",
                address = "Sector 4",
                village = "Beri",
                city = "Jhajjar",
                district = "Jhajjar",
                state = "Haryana",
                pinCode = "124201",
                addedByAdmin = "Vikramaditya Sengupta"
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = ChandaRepository(firestore)
        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getDonorById(donorId)
        }
        assertTrue("Cross-user access should fail", result.isFailure)
        val firestoreEx = result.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull("Expected FirebaseFirestoreException", firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    @Test
    fun observeDonors_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = ChandaRepository(firestore)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeDonors("unauthenticated_uid").first()
            }
            fail("Expected exception for unauthenticated user")
        } catch (e: Exception) {
            val firestoreException = checkNotNull(e.unwrapFirestoreException()) {
                "Expected nested FirebaseFirestoreException, got $e"
            }
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice.admin@akhada.org"
        const val BOB_EMAIL = "bob.admin@akhada.org"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
