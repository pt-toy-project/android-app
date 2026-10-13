package com.example.test.Hero

/** Original editorial prompts; these never claim to reconstruct a fixed historical day. */
data class DayEditorialProfile(val learning: String, val walking: String, val rest: String)

object DayEditorialProfiles {
    private val profiles = mapOf(
        "sejong" to DayEditorialProfile("어려운 설명을 누구나 이해할 짧은 문장으로 다시 써 봅니다.", "주변의 안내문에서 읽기 어려운 표현을 하나 찾습니다.", "교정한 문장에서 눈을 떼고 멀리 바라봅니다."),
        "yi_sun_sin" to DayEditorialProfile("오늘 필요한 준비와 실제 상태를 구분해 점검표를 만듭니다.", "안전한 길을 걸으며 이동 동선과 위험 요소를 살핍니다.", "점검표를 내려놓고 어깨와 손의 긴장을 풉니다."),
        "einstein" to DayEditorialProfile("익숙한 현상 하나에 왜 그런지 질문하고 설명을 찾아봅니다.", "빛·소리·움직임 중 눈에 띈 현상을 말로 설명합니다.", "음악을 조용히 듣거나 악기 없이 리듬을 느끼며 쉽니다."),
        "newton" to DayEditorialProfile("추측 하나를 계산이나 자료 대조로 확인해 봅니다.", "그림자와 빛의 방향을 안전하게 관찰합니다. 태양을 직접 보지 않습니다.", "계산을 멈추고 눈과 손을 쉬게 합니다."),
        "da_vinci" to DayEditorialProfile("사물 하나를 관찰하고 작은 스케치에 특징을 붙입니다.", "나뭇잎이나 건물의 형태에서 반복되는 구조를 찾습니다.", "그림 도구를 내려놓고 자세와 손목을 편하게 합니다."),
        "shakespeare" to DayEditorialProfile("일상의 장면 하나를 자신의 비유로 표현합니다. 작품의 시구는 복사하지 않습니다.", "사람을 방해하지 않고 거리의 분위기와 움직임을 살핍니다.", "쓰던 문장을 덮고 창밖의 장면을 잠시 바라봅니다."),
        "franklin" to DayEditorialProfile("오늘 연습할 덕목 하나를 작은 행동으로 정합니다.", "오늘 세운 계획을 지키기 어려웠던 이유를 차분히 생각합니다.", "계획을 모두 지키지 못했어도 여유를 두고 쉽니다."),
        "darwin" to DayEditorialProfile("관찰한 사실과 이를 설명하는 가설을 따로 적습니다.", "안전한 길에서 식물의 차이를 살피고 날짜와 장소를 적습니다.", "관찰 기록을 멈추고 몸 상태에 맞게 휴식합니다."),
        "curie" to DayEditorialProfile("같은 기준으로 측정할 수 있는 안전한 생활 지표를 정합니다.", "동행이 있다면 관찰한 풍경 하나를 서로 나눕니다.", "측정과 비교를 잠시 멈추고 몸의 피로를 살핍니다."),
        "nightingale" to DayEditorialProfile("작업 공간의 공기·빛·소음을 관찰의 문장으로 적습니다.", "주변 환경이 몸과 마음에 주는 영향을 살핍니다.", "화면을 끄고 조용하고 편안한 공간에서 쉽니다."),
        "marcus_aurelius" to DayEditorialProfile("내가 바꿀 수 있는 행동과 바꿀 수 없는 상황을 나눕니다.", "걷는 동안 오늘 맡은 일과 주변 사람의 입장을 생각합니다.", "해결하지 못한 문제를 잠시 내려놓고 호흡을 가다듬습니다."),
        "thoreau" to DayEditorialProfile("읽은 한 문단을 생활의 경험과 연결해 내 말로 씁니다.", "소리와 계절의 작은 변화를 안전하게 관찰합니다.", "일을 멈추고 지금 꼭 필요한 것과 덜 필요한 것을 생각합니다."),
        "douglass" to DayEditorialProfile("낯선 글을 읽고 이해한 내용을 다른 사람에게 설명합니다.", "내가 배울 수 있는 기회를 늘릴 방법을 생각합니다.", "읽고 쓰는 일을 쉬며 손과 눈의 긴장을 풉니다."),
        "john_muir" to DayEditorialProfile("자연 관찰 기록에서 사실과 감상을 구분합니다.", "익숙한 나무나 풍경을 관찰하고 특징을 간단히 그립니다.", "관찰 결과를 평가하지 않고 바깥 풍경을 바라보며 쉽니다."),
        "jane_addams" to DayEditorialProfile("이웃의 필요를 내가 짐작한 것과 실제 들은 것으로 나눕니다.", "주변의 공동체 공간과 참여할 수 있는 활동을 알아봅니다.", "다른 사람을 돕는 일과 나의 휴식 사이에 여유를 둡니다."),
        "jeong_yak_yong" to DayEditorialProfile("생활의 불편 하나를 원인·개선안·실행 단계로 나눕니다.", "주변 시설이 이용하기 편한지 사용자의 입장에서 살핍니다.", "해결책을 서두르지 않고 작업에서 잠시 떨어져 쉽니다."),
        "yi_hwang" to DayEditorialProfile("내가 중요하게 생각하는 원칙을 실제 행동과 연결합니다.", "걷는 동안 산만해진 마음을 가다듬고 주변을 살핍니다.", "집중을 잠시 멈추고 자세와 호흡을 편안하게 합니다."),
        "yi_i" to DayEditorialProfile("오늘 배울 이유와 작은 실천 목표를 내 말로 씁니다.", "배운 원칙을 일상에서 실천할 수 있는 장면을 찾습니다.", "배움의 속도를 재촉하지 않고 잠시 쉽니다."),
        "heo_jun" to DayEditorialProfile("건강 정보의 작성자와 근거를 확인합니다. 옛 처방을 재현하지 않습니다.", "몸 상태에 맞게 가볍게 걷고 무리하지 않습니다.", "피로와 수면 상태를 살피고 필요하면 전문가에게 문의합니다."),
        "confucius" to DayEditorialProfile("어제 배운 내용을 다시 설명하고 모르는 부분을 표시합니다.", "함께 사는 사람에게 배울 점 하나를 생각합니다.", "배움의 성과를 비교하지 않고 편안하게 쉽니다."),
        "zhu_xi" to DayEditorialProfile("한 개념의 뜻을 자료로 확인하고 질문을 남깁니다.", "생각한 원칙이 구체적인 생활 장면과 어떻게 연결되는지 살핍니다.", "읽던 글에서 잠시 떨어져 마음을 가다듬습니다."),
        "xu_xiake" to DayEditorialProfile("지도와 실제 관찰을 비교해 경로의 특징을 정리합니다.", "안전한 가까운 길에서 지형과 날씨를 관찰합니다.", "이동 뒤 몸 상태를 살피고 관찰 도구를 정돈합니다."),
        "matsuo_basho" to DayEditorialProfile("오늘 본 장면을 짧은 자신의 산문으로 남깁니다.", "계절을 드러내는 소리나 색을 찾습니다. 위험한 여행은 따라 하지 않습니다.", "문장의 완성도를 따지지 않고 주변 풍경을 바라봅니다."),
        "ada_lovelace" to DayEditorialProfile("작업을 입력·처리·결과로 나누어 작은 예를 확인합니다.", "반복되는 생활 동작에서 순서와 규칙을 찾아봅니다.", "복잡한 절차를 잠시 내려놓고 눈과 몸을 쉽니다."),
        "maria_merian" to DayEditorialProfile("관찰한 생물의 형태를 그림과 짧은 설명으로 남깁니다.", "생물을 잡거나 해치지 않고 식물과 곤충을 관찰합니다.", "관찰 도구를 정리하고 눈과 손을 쉬게 합니다."),
        "ibn_battuta" to DayEditorialProfile("다른 지역의 생활 자료를 읽고 내 경험과 구분합니다.", "익숙한 동네에서 내가 놓쳤던 장소와 생활 모습을 찾습니다.", "새로운 경험을 판단하지 않고 천천히 되짚습니다."),
        "ibn_al_haytham" to DayEditorialProfile("안전한 관찰에서 한 조건만 바꾸어 결과를 비교합니다.", "그림자와 반사를 살핍니다. 태양이나 강한 빛을 직접 보지 않습니다.", "관찰과 실험을 멈추고 눈을 편하게 쉬게 합니다."),
        "tagore" to DayEditorialProfile("자연과 사람의 관계에 대한 생각을 자신의 산문으로 씁니다.", "주변 자연을 살피며 내가 연결된 사람과 장소를 떠올립니다.", "글쓰기와 평가를 잠시 멈추고 조용히 쉽니다."),
        "sor_juana" to DayEditorialProfile("읽는 주제를 다른 분야와 연결하는 질문을 만듭니다.", "배움의 기회를 나와 다른 사람에게 넓힐 방법을 생각합니다.", "이해하지 못한 부분을 남겨 둔 채 잠시 쉽니다."),
        "montessori" to DayEditorialProfile("집중을 돕는 도구와 환경을 관찰하고 작은 개선을 시도합니다.", "내가 스스로 선택할 수 있는 활동과 공간을 살핍니다.", "성과를 판단하기 전에 피로와 집중 상태를 관찰하며 쉽니다."),
    )
    fun forHero(id: String): DayEditorialProfile = requireNotNull(profiles[id]) { "Missing day profile: $id" }
}
