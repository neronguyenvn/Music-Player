package org.hyperskill.musicplayer.feature.music

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.hyperskill.musicplayer.R
import org.hyperskill.musicplayer.databinding.FragmentMainPlayerBinding

class MainPlayerControllerFragment(private val viewModel: MainViewModel) : Fragment() {

    private var _binding: FragmentMainPlayerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.controllerBtnPlayPause.setOnClickListener {
            viewModel.playPause()
        }
        binding.controllerBtnStop.setOnClickListener {
            viewModel.stop()
        }
    }
}