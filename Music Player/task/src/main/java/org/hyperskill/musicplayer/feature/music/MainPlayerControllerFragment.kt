package org.hyperskill.musicplayer.feature.music

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import org.hyperskill.musicplayer.databinding.FragmentMainPlayerBinding
import org.hyperskill.musicplayer.helper.formatMilliseconds

class MainPlayerControllerFragment(
    private val viewModel: MainViewModel,
) : Fragment() {

    private var _binding: FragmentMainPlayerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainPlayerBinding.inflate(inflater, container, false)
        binding.controllerBtnPlayPause.setOnClickListener {
            viewModel.onPlayOrPauseClick()
        }
        binding.controllerBtnStop.setOnClickListener {
            viewModel.onStopClick()
        }
        binding.controllerSeekBar.setOnSeekBarChangeListener(
            object : OnSeekBarChangeListener {
                override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                    binding.controllerTvCurrentTime.text = formatMilliseconds(p1 * 1000)
                }

                override fun onStartTrackingTouch(p0: SeekBar?) {
                    viewModel.onTouchStart()
                }

                override fun onStopTrackingTouch(p0: SeekBar?) {
                    viewModel.onTouchStop((p0?.progress?.times(1000)) ?: 0)
                }
            }
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.playerControllerUiState.collect {
                    binding.controllerSeekBar.max = (it.currentTrack?.song?.duration ?: 0) / 1000
                    binding.controllerSeekBar.progress = it.currentPosition / 1000
                    binding.controllerTvTotalTime.text =
                        formatMilliseconds(it.currentTrack?.song?.duration ?: 0)
                }
            }
        }
    }
}