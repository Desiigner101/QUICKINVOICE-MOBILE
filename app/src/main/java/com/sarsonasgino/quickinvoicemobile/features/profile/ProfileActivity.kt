package com.sarsonasgino.quickinvoicemobile.features.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.util.Base64
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.sarsonasgino.quickinvoicemobile.R
import com.sarsonasgino.quickinvoicemobile.databinding.ActivityProfileBinding
import com.sarsonasgino.quickinvoicemobile.features.login.LoginActivity

class ProfileActivity : AppCompatActivity(), ProfileContract.View {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var presenter: ProfilePresenter
    private var isNewPasswordVisible = false
    private var isConfirmPasswordVisible = false

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleImageSelected(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        presenter = ProfilePresenter(this, this)

        setupListeners()
        presenter.loadProfile()
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onDestroy()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }

        // Photo picker
        binding.btnChangePhoto.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // Save profile
        binding.btnSaveProfile.setOnClickListener {
            val firstName = binding.etFirstName.text.toString().trim()
            val lastName = binding.etLastName.text.toString().trim()
            presenter.onUpdateProfile(firstName, lastName, "", "")
        }

        // Change password
        binding.btnChangePassword.setOnClickListener {
            val password = binding.etNewPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()
            val firstName = binding.etFirstName.text.toString().trim()
            val lastName = binding.etLastName.text.toString().trim()
            presenter.onUpdateProfile(firstName, lastName, password, confirmPassword)
        }

        // Delete account
        binding.btnDeleteAccount.setOnClickListener {
            presenter.onDeleteAccount()
        }

        // Toggle new password
        binding.btnToggleNewPassword.setOnClickListener {
            isNewPasswordVisible = !isNewPasswordVisible
            if (isNewPasswordVisible) {
                binding.etNewPassword.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                binding.btnToggleNewPassword.setImageResource(R.drawable.ic_eye_on)
            } else {
                binding.etNewPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                binding.btnToggleNewPassword.setImageResource(R.drawable.ic_eye_off)
            }
            binding.etNewPassword.setSelection(binding.etNewPassword.text.length)
        }

        // Toggle confirm password
        binding.btnToggleConfirmPassword.setOnClickListener {
            isConfirmPasswordVisible = !isConfirmPasswordVisible
            if (isConfirmPasswordVisible) {
                binding.etConfirmPassword.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                binding.btnToggleConfirmPassword.setImageResource(R.drawable.ic_eye_on)
            } else {
                binding.etConfirmPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                binding.btnToggleConfirmPassword.setImageResource(R.drawable.ic_eye_off)
            }
            binding.etConfirmPassword.setSelection(binding.etConfirmPassword.text.length)
        }
    }

    private fun handleImageSelected(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()

            if (bytes != null) {
                val base64 = Base64.encodeToString(bytes, Base64.DEFAULT)
                val base64WithPrefix = "data:image/jpeg;base64,$base64"
                presenter.onPhotoSelected(base64WithPrefix)

                // Show preview
                binding.tvProfileInitial.visibility = View.GONE
                binding.ivProfilePhoto.visibility = View.VISIBLE
                Glide.with(this).load(uri).circleCrop().into(binding.ivProfilePhoto)
            }
        } catch (e: Exception) {
            showError("Failed to load image")
        }
    }

    // --- ProfileContract.View implementations ---

    override fun showLoading() {
        binding.loadingOverlay.visibility = View.VISIBLE
    }

    override fun hideLoading() {
        binding.loadingOverlay.visibility = View.GONE
    }

    override fun showProfile(firstName: String, lastName: String, email: String, photoUrl: String) {
        binding.etFirstName.setText(firstName)
        binding.etLastName.setText(lastName)
        binding.etEmail.setText(email)
        binding.tvProfileName.text = "$firstName $lastName"
        binding.tvProfileEmail.text = email

        if (photoUrl.isNotEmpty()) {
            binding.tvProfileInitial.visibility = View.GONE
            binding.ivProfilePhoto.visibility = View.VISIBLE
            Glide.with(this).load(photoUrl).circleCrop().into(binding.ivProfilePhoto)
        } else {
            binding.tvProfileInitial.visibility = View.VISIBLE
            binding.ivProfilePhoto.visibility = View.GONE
            binding.tvProfileInitial.text = firstName.take(1).uppercase()
        }
    }

    override fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun showPasswordMismatch() {
        Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show()
        binding.etConfirmPassword.error = "Passwords do not match"
    }

    override fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    override fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("⚠️ Delete Account")
            .setMessage("Are you sure you want to delete your account? This action cannot be undone. All your invoices will be permanently deleted.")
            .setPositiveButton("Delete Forever") { _, _ ->
                presenter.onConfirmDelete()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}