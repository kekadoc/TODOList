package com.kekadoc.test.todolist.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.kekadoc.test.todolist.R
import com.kekadoc.test.todolist.RepoViewModel
import com.kekadoc.test.todolist.databinding.FragmentLoginBinding
import com.kekadoc.test.todolist.auth.Credential

/**
 * Login fragment
 */
class LoginFragment : Fragment() {

    companion object {
        private const val TAG: String = "LoginFragment-TAG"
    }

    private val repoViewModel by activityViewModels<RepoViewModel>()

    private lateinit var binding: FragmentLoginBinding

    /**
     * Name validity check
     *
     * @return current name
     */
    private fun checkName(): String? {
        val name = binding.editTextName.text?.toString()
        binding.editTextName.error =
            if (name.isNullOrEmpty()) getString(R.string.fragment_login_text_input_name_error_empty) else null
        return name
    }

    /**
     * Password validation check
     *
     * @return current password
     */
    private fun checkPassword(): String? {
        val password = binding.editTextPassword.text?.toString()
        binding.editTextPassword.error =
            if (password.isNullOrEmpty()) getString(R.string.fragment_login_text_input_password_error_empty) else null
        return password
    }

    /**
     * Checking the validity of the entered data
     *
     * @return null if not valid 
     * 
     * @see checkName
     * @see checkPassword
     */
    private fun checkCredential(): Credential? {
        val name = checkName()
        val password = checkPassword()
        return if (name.isNullOrEmpty() || password.isNullOrEmpty()) null
        else Credential(name, password)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.editTextName.addTextChangedListener {
            checkName()
        }
        binding.editTextPassword.addTextChangedListener {
            checkPassword()
        }
        binding.buttonSignIn.setOnClickListener {
            val credential = checkCredential()
            if (credential != null) {
                /**
                 * @see [com.kekadoc.test.todolist.MainActivity.onCreate]]
                 */
                repoViewModel.logIn(credential)
            }
        }
    }

}