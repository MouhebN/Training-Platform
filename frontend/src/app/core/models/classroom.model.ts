export interface ClassroomContext {
  sessionId: number;
  sessionTitle: string;
  jitsiDomain: string;
  roomName: string;
  displayName: string;
  moderator: boolean;
  heartbeatIntervalSec: number;
  attendanceThresholdPercent: number;
}

export interface ClassroomAttendanceEntry {
  enrollmentId: number;
  learnerId: number;
  learnerFullName: string;
  enrollmentStatus: string;
  connected: boolean;
  trackedSeconds: number;
  trainerActiveSeconds: number;
  attendancePercentage: number;
  qualified: boolean;
}

export interface ClassroomAttendanceReport {
  sessionId: number;
  trainerActiveSeconds: number;
  attendanceThresholdPercent: number;
  learners: ClassroomAttendanceEntry[];
}

declare global {
  interface Window {
    JitsiMeetExternalAPI?: new (
      domain: string,
      options: {
        roomName: string;
        parentNode: HTMLElement;
        width?: string | number;
        height?: string | number;
        userInfo?: { displayName?: string };
        configOverwrite?: Record<string, unknown>;
        interfaceConfigOverwrite?: Record<string, unknown>;
      }
    ) => {
      dispose: () => void;
      addListener: (event: string, listener: () => void) => void;
    };
  }
}

export {};
