// 1. 유저와 루틴 클래스 (순수 데이터 모델)
class User(val id: Long, var coinBalance: Int = 0) {
    fun addCoins(amount: Int) {
if (amount < 0) throw IllegalArgumentException("지급할 코인은 0 이상이어야 합니다.")
coinBalance += amount
println("💰 유저 $id: $amount 코인 획득! (현재 총 잔액: $coinBalance)")
}
}

class Routine(val id: Long, val userId: Long, var isCompleted: Boolean = false, val rewardCoins: Int = 10) {
fun markAsCompleted() {
if (isCompleted) throw IllegalStateException("이미 완료 처리된 루틴입니다.")
isCompleted = true
println("✅ 루틴 $id: 완료 처리 완료.")
}
}

// 2. 실행 가능한 메인 함수
fun main() {
// 가짜 데이터 생성 (DB에 데이터가 있다고 가정)
val testUser = User(id = 1L, coinBalance = 0)
val testRoutine = Routine(id = 100L, userId = 1L, rewardCoins = 50)

    println("--- 초기 상태 ---")
    println("유저 코인: ${testUser.coinBalance}")
    println("루틴 상태: ${if (testRoutine.isCompleted) "완료" else "미완료"}\n")

    println("--- 루틴 완료 시도 ---")
    try {
        // 서버 서비스 로직 시뮬레이션
        if (testRoutine.userId == testUser.id) {
            testRoutine.markAsCompleted()     // 1. 루틴 완료 처리
            testUser.addCoins(testRoutine.rewardCoins) // 2. 코인 지급
        } else {
            println("❌ 권한이 없습니다.")
        }
    } catch (e: Exception) {
        println("❌ 에러 발생: ${e.message}")
    }

    println("\n--- 최종 결과 확인 ---")
    println("유저 코인: ${testUser.coinBalance}")
    println("루틴 상태: ${if (testRoutine.isCompleted) "완료" else "미완료"}")
}