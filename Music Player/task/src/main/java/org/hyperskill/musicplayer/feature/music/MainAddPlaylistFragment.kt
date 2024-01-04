package org.hyperskill.musicplayer.feature.music

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.hyperskill.musicplayer.R
import org.hyperskill.musicplayer.databinding.ActivityMainBinding
import org.hyperskill.musicplayer.databinding.FragmentAddPlaylistBinding

class MainAddPlaylistFragment(private val viewModel: MainViewModel) : Fragment() {

    private var _binding: FragmentAddPlaylistBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddPlaylistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.addPlaylistBtnOk.setOnClickListener {
            val errorString = viewModel.onAddOkClick()
            if (errorString.isNotEmpty()) Toast.makeText(
                this.context, errorString,
                Toast.LENGTH_SHORT
            ).show()
        }
        binding.addPlaylistEtPlaylistName.addTextChangedListener(onTextChanged = { text, _, _, _ ->
            viewModel.onPlaylistNameChange(text.toString())
        })
        binding.addPlaylistBtnCancel.setOnClickListener {
            viewModel.onCancelClick()
        }
        binding.addPlaylistEtPlaylistName.setText(viewModel.addPlaylistEtPlaylistName)
    }
}