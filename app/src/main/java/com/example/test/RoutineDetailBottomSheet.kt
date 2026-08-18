package com.example.test

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class RoutineDetailBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // dialog_routine_detail 레이아웃을 연결합니다.
        return inflater.inflate(R.layout.dialog_routine_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnStart: View = view.findViewById(R.id.btn_start)
        val btnRemindLater: View = view.findViewById(R.id.btn_remind_later)

        btnStart.setOnClickListener {
            Toast.makeText(requireContext(), "루틴을 시작합니다!", Toast.LENGTH_SHORT).show()
            dismiss()
        }

        btnRemindLater.setOnClickListener {
            Toast.makeText(requireContext(), "5분 후 알림을 설정했습니다.", Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }
}