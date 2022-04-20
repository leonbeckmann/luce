package healthcare

class RecordHandle(
    private val record: PatientRecord
) {

    fun drop() : Boolean {
        return true
    }

}