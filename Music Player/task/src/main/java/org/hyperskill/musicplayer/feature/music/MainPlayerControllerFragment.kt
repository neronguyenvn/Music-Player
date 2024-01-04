package org.hyperskill.musicplayer.feature.music

import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.hyperskill.musicplayer.databinding.FragmentMainPlayerBinding
import org.hyperskill.musicplayer.helper.formatMilliseconds

class MainPlayerControllerFragment(private val viewModel: MainViewModel) : Fragment() {

    private var _binding: FragmentMainPlayerBinding? = null
    private val binding get() = _binding!!

    private var mediaPlayer: MediaPlayer? = null

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
            viewModel.onPlayOrPauseClick()
        }
        binding.controllerBtnStop.setOnClickListener {
            viewModel.onStopClick()
        }
        viewModel.playerControllerUiState.value.apply {
            binding.controllerTvTotalTime.text =
                formatMilliseconds(currentTrack?.song?.duration ?: 0)
            binding.controllerSeekBar.max = currentTrack?.song?.duration ?: 0
            binding.controllerSeekBar.progress = currentPosition
            binding.controllerSeekBar.setOnSeekBarChangeListener(
                object : OnSeekBarChangeListener {
                    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                        viewModel.onControllerSeekBarChange(p1)
                    }

                    override fun onStartTrackingTouch(p0: SeekBar?) {}
                    override fun onStopTrackingTouch(p0: SeekBar?) {}
                }
            )
        }
        lifecycleScope.launch {
            viewModel.playerControllerUiState.collect {
                binding.controllerTvCurrentTime.text = formatMilliseconds(it.currentPosition)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}