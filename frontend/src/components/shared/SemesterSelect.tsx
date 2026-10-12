import { useEffect } from 'react'
import { useSemesters } from "@/lib/catalogQueries"
import { usePlanner } from "@/features/planner/plannerStore"
import { Select } from '@/components/ui/input'

export function SemesterSelect() {
  const query =useSemesters()
  const semesterId =usePlanner((state) => state.semesterId)
  const setSemester =usePlanner((state) => state.setSemester)
  useEffect(() => {
    if (!semesterId && query.data?.length) {
      const today =new Date().toLocaleDateString('en-CA')
      const active =query.data.find((semester) => semester.startsOn <=today && semester.endsOn >=today)
      setSemester((active ?? query.data[0]).id)
    }
  }, [semesterId, query.data, setSemester])
  return <Select aria-label="Select semester" value={semesterId} onChange={(event) => setSemester(event.target.value)} disabled={!query.data?.length} className="sm:w-72"><option value="" disabled>{query.isPending ? 'Loading semesters…' : query.isError ? 'Semesters unavailable' : 'Choose a semester'}</option>{query.data?.map((semester) => <option key={semester.id} value={semester.id}>{semester.name}</option>)}</Select>
}
