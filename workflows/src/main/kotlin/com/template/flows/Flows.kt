package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.conclave.client.EnclaveConstraint
import com.r3.conclave.common.EnclaveInstanceInfo
import com.r3.conclave.mail.Curve25519KeyPairGenerator
import net.corda.core.crypto.keys
import net.corda.core.flows.*
import net.corda.core.utilities.ProgressTracker
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.util.*


// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class Initiator : FlowLogic<Unit>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call() {

        // Connect to the host, it will send us a remote attestation (EnclaveInstanceInfo).
        var fromHost: DataInputStream
        var toHost: DataOutputStream
        val numberOfDice = 5

        while (true) {
            try {
                println("Attempting to connect to localhost:9000")
                val socket = Socket()
                socket.connect(InetSocketAddress(InetAddress.getLoopbackAddress(), 9000), 5000)
                fromHost = DataInputStream(socket.getInputStream())
                toHost = DataOutputStream(socket.getOutputStream())
                break
            } catch (e: Exception) {
                System.err.println("Retrying: " + e.message)
                Thread.sleep(2000)
            }
        }

        val attestationBytes = ByteArray(fromHost.readInt())
        fromHost.readFully(attestationBytes)
        //val attestation = EnclaveInstanceInfo.deserialize(attestationBytes)

        // Check it's the enclave we expect. This will throw InvalidEnclaveException if not valid.

        // Check it's the enclave we expect. This will throw InvalidEnclaveException if not valid.
        //println("Connected to $attestation")
        //EnclaveConstraint.parse("S:4924CA3A9C8241A3C0AA1A24A407AA86401D2B79FA9FF84932DA798A942166D4 PROD:1 SEC:INSECURE").check(attestation)

        // Generate our own Curve25519 keypair so we can receive a response.

        // Generate our own Curve25519 keypair so we can receive a response.
        //val myKey = Curve25519KeyPairGenerator().generateKeyPair()
        val myKey = serviceHub.keyManagementService.freshKey()
        // Now we checked the enclave's identity and are satisfied it's the enclave from this project,
        // we can send mail to it. We will provide our own private key whilst encrypting, so the enclave
        // gets our public key and can encrypt a reply.


        // Now we checked the enclave's identity and are satisfied it's the enclave from this project,
        // we can send mail to it. We will provide our own private key whilst encrypting, so the enclave
        // gets our public key and can encrypt a reply.
        val inputBytes = ByteArray(Integer.SIZE)
        ByteBuffer.wrap(inputBytes).putInt(numberOfDice)

        val mail = attestation.createMail(inputBytes)
        mail.privateKey = myKey.keys
        // Set a random topic, so we can re-run this program against the same server.
        // Set a random topic, so we can re-run this program against the same server.
        mail.topic = UUID.randomUUID().toString()
        val encryptedMail = mail.encrypt()

        println("Sending the encrypted mail to the host.")

        toHost.writeInt(encryptedMail.size)
        toHost.write(encryptedMail)

        // Enclave will mail us back.

        // Enclave will mail us back.
        val encryptedReply = ByteArray(fromHost.readInt())
        println("Reading reply mail of length " + encryptedReply.size + " bytes.")
        fromHost.readFully(encryptedReply)
        val reply = attestation.decryptMail(encryptedReply, myKey.private)

        println("Reading the dice of this player")

        val results: IntArray = convertByteArrayToIntArray(reply.bodyAsBytes)

        for (i in 0..4) {
            println(results[i])
        }

        toHost.close()
        fromHost.close()


    }

    private fun convertByteArrayToInt(intBytes: ByteArray): Int {
        val byteBuffer = ByteBuffer.allocate(Integer.SIZE)
        byteBuffer.put(intBytes)
        byteBuffer.flip()
        return byteBuffer.int
    }

    fun convertByteArrayToIntArray(data: ByteArray): IntArray {

        if (data == null || data.size % Integer.SIZE != 0) return IntArray(0)

        val ints = IntArray(data.size / Integer.SIZE)
        val bytesOfInt = ByteArray(Integer.SIZE)

        for (i in ints.indices) {
            for (j in 0 until Integer.SIZE) {
                bytesOfInt[j] = data[i * Integer.SIZE + j]
            }
            ints[i] = convertByteArrayToInt(bytesOfInt)
        }

        return ints
    }

}

@InitiatedBy(Initiator::class)
class Responder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        // Responder flow logic goes here.
    }
}


