package org.hyperskill.musicplayer.feature.music

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.hyperskill.musicplayer.databinding.FragmentMainPlayerBinding
import org.hyperskill.musicplayer.helper.formatMilliseconds

class MainPlayerControllerFragment(
    private val viewModel: MainViewModel,
) : Fragment() {

    private var _binding: FragmentMainPlayerBinding? = null
    private val binding get() = _binding!!

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
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
                    viewModel.onControllerSeekBarChange(p1)
                }

                override fun onStartTrackingTouch(p0: SeekBar?) {}
                override fun onStopTrackingTouch(p0: SeekBar?) {
                    viewModel.onStopTouch(p0?.progress ?: 0)
                }
            }
        )
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        lifecycleScope.launch {
            viewModel.playerControllerUiState.collect {
                binding.controllerTvCurrentTime.text = formatMilliseconds(it.currentPosition)
                binding.controllerSeekBar.max = it.currentTrack?.song?.duration ?: 0
                binding.controllerSeekBar.progress = it.currentPosition
                binding.controllerTvTotalTime.text =
                    formatMilliseconds(it.currentTrack?.song?.duration ?: 0)
            }
        }
    }
}