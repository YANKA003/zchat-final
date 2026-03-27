package com.zchat.app.utils

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.util.Log
import androidx.core.content.ContextCompat
import com.zchat.app.data.model.User

object ContactsHelper {
    
    private const val TAG = "ContactsHelper"
    
    fun hasContactsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    fun importContacts(context: Context): List<ContactInfo> {
        val contacts = mutableListOf<ContactInfo>()
        
        if (!hasContactsPermission(context)) {
            Log.w(TAG, "No contacts permission")
            return contacts
        }
        
        val contentResolver: ContentResolver = context.contentResolver
        
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        
        try {
            val cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )
            
            cursor?.use {
                val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                
                val addedNumbers = mutableSetOf<String>()
                
                while (it.moveToNext()) {
                    val name = it.getString(nameIndex)
                    val rawNumber = it.getString(numberIndex)
                    
                    // Normalize phone number
                    val number = normalizePhoneNumber(rawNumber)
                    
                    if (number.isNotEmpty() && !addedNumbers.contains(number)) {
                        addedNumbers.add(number)
                        contacts.add(ContactInfo(
                            name = name,
                            phoneNumber = number,
                            rawNumber = rawNumber
                        ))
                    }
                }
            }
            
            Log.d(TAG, "Imported ${contacts.size} contacts")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error importing contacts", e)
        }
        
        return contacts
    }
    
    private fun normalizePhoneNumber(number: String): String {
        // Remove all non-digit characters
        val digits = number.filter { it.isDigit() }
        
        // Handle different formats
        return when {
            digits.isEmpty() -> ""
            digits.length >= 10 -> {
                // Get last 10-15 digits (most phone numbers)
                val cleanNumber = digits.takeLast(15)
                // Add + prefix for international format
                if (cleanNumber.startsWith("375")) "+$cleanNumber"
                else if (cleanNumber.startsWith("380")) "+$cleanNumber"
                else if (cleanNumber.startsWith("7") && cleanNumber.length == 11) "+$cleanNumber"
                else if (cleanNumber.startsWith("8") && cleanNumber.length == 11) "+7${cleanNumber.substring(1)}"
                else "+$cleanNumber"
            }
            else -> digits
        }
    }
    
    fun findMatchingUsers(contacts: List<ContactInfo>, registeredUsers: List<User>): List<User> {
        val contactNumbers = contacts.map { it.phoneNumber }.toSet()
        return registeredUsers.filter { user ->
            val userPhone = normalizePhoneNumber(user.phoneNumber)
            contactNumbers.contains(userPhone) || contactNumbers.any { it.contains(userPhone.takeLast(10)) }
        }
    }
}

data class ContactInfo(
    val name: String,
    val phoneNumber: String,
    val rawNumber: String
)
