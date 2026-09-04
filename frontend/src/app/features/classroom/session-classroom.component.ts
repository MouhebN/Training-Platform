import { Component, ElementRef, OnDestroy, OnInit, ViewChild, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { ClassroomAttendanceReport, ClassroomContext } from '../../core/models/classroom.model';
import { TrainingSession } from '../../core/models/session.model';
import { ClassroomService } from '../../core/services/classroom.service';
import { ConfirmDialogService } from '../../core/services/confirm-dialog.service';
import { SessionService } from '../../core/services/session.service';

@Component({
  selector: 'app-session-classroom',
  standalone: true,
  imports: [RouterLink],
  styles: [`
    :host {
      display: block;
      margin: -30px;
      min-height: calc(100dvh - 60px);
    }

    .meet-root {
      min-height: calc(100dvh - 60px);
      background: #131314;
      color: #e8eaed;
      display: flex;
      flex-direction: column;
      overflow: hidden;
    }

    .meet-toolbar {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
      padding: 12px 20px;
      background: rgba(19, 19, 20, .96);
      border-bottom: 1px solid rgba(255, 255, 255, .08);
      backdrop-filter: blur(12px);
      z-index: 5;
      flex-shrink: 0;
    }

    .meet-brand {
      display: flex;
      align-items: center;
      gap: 14px;
      min-width: 0;
    }

    .meet-logo {
      width: 40px;
      height: 40px;
      border-radius: 12px;
      background: linear-gradient(135deg, #e66239 0%, #c2410c 100%);
      display: grid;
      place-items: center;
      font-size: 16px;
      flex-shrink: 0;
    }

    .meet-brand-copy { min-width: 0; }

    .meet-brand-copy h1 {
      margin: 0;
      font-size: 16px;
      font-weight: 600;
      color: #fff;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .meet-brand-copy p {
      margin: 2px 0 0;
      font-size: 12px;
      color: #9aa0a6;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .meet-toolbar-actions {
      display: flex;
      align-items: center;
      gap: 10px;
      flex-shrink: 0;
    }

    .meet-chip {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 6px 12px;
      border-radius: 999px;
      font-size: 12px;
      font-weight: 500;
    }

    .meet-chip.live {
      color: #81c995;
      border: 1px solid rgba(129, 201, 149, .25);
      background: rgba(129, 201, 149, .08);
    }

    .meet-chip.feature {
      color: #c4b5fd;
      border: 1px solid rgba(196, 181, 253, .2);
      background: rgba(196, 181, 253, .08);
    }

    .live-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: #81c995;
      animation: pulse 2s infinite;
    }

    @keyframes pulse {
      70% { box-shadow: 0 0 0 8px rgba(129, 201, 149, 0); }
      100% { box-shadow: 0 0 0 0 rgba(129, 201, 149, 0); }
    }

    .meet-btn {
      appearance: none;
      border: none;
      border-radius: 999px;
      padding: 10px 18px;
      font: inherit;
      font-size: 13px;
      font-weight: 500;
      cursor: pointer;
      transition: background .15s ease;
    }

    .meet-btn-ghost {
      background: rgba(255, 255, 255, .08);
      color: #e8eaed;
    }

    .meet-btn-ghost:hover { background: rgba(255, 255, 255, .12); }
    .meet-btn-ghost.active { background: rgba(138, 180, 248, .18); color: #8ab4f8; }

    .meet-btn-danger { background: #ea4335; color: #fff; }
    .meet-btn-danger:hover { background: #d93025; }
    .meet-btn-primary { background: #e66239; color: #fff; width: 100%; }
    .meet-btn-primary:hover { background: #d4562f; }

    .meet-stage {
      flex: 1;
      min-height: 0;
      display: grid;
      grid-template-columns: minmax(0, 1fr);
      position: relative;
    }

    .meet-stage.with-panel { grid-template-columns: minmax(0, 1fr) 300px; }

    .meet-video-wrap {
      position: relative;
      min-height: 0;
      background: #000;
    }

    .meet-video {
      position: absolute;
      inset: 0;
      width: 100%;
      height: 100%;
    }

    .meet-video ::ng-deep iframe {
      width: 100% !important;
      height: 100% !important;
      border: 0;
    }

    .meet-loading {
      position: absolute;
      inset: 0;
      display: grid;
      place-content: center;
      gap: 16px;
      background: radial-gradient(circle at top, #1f1f20 0%, #131314 55%);
      z-index: 2;
      text-align: center;
      padding: 24px;
    }

    .meet-spinner {
      width: 44px;
      height: 44px;
      border-radius: 50%;
      border: 3px solid rgba(255, 255, 255, .12);
      border-top-color: #e66239;
      animation: spin .8s linear infinite;
      margin: 0 auto;
    }

    @keyframes spin { to { transform: rotate(360deg); } }

    .meet-loading h2 { margin: 0; font-size: 20px; font-weight: 500; color: #fff; }
    .meet-loading p { margin: 0; color: #9aa0a6; max-width: 300px; font-size: 14px; }

    .meet-toast {
      position: absolute;
      top: 16px;
      left: 50%;
      transform: translateX(-50%);
      z-index: 6;
      padding: 12px 18px;
      border-radius: 12px;
      font-size: 13px;
      max-width: min(480px, calc(100% - 32px));
      box-shadow: 0 12px 40px rgba(0, 0, 0, .35);
    }

    .meet-toast.error { background: rgba(234, 67, 53, .95); color: #fff; }
    .meet-toast.success { background: rgba(52, 168, 83, .95); color: #fff; }

    .learner-note {
      position: absolute;
      bottom: 20px;
      left: 50%;
      transform: translateX(-50%);
      z-index: 3;
      padding: 10px 16px;
      border-radius: 999px;
      background: rgba(32, 33, 36, .88);
      border: 1px solid rgba(255, 255, 255, .1);
      color: #bdc1c6;
      font-size: 12px;
      backdrop-filter: blur(8px);
      pointer-events: none;
    }

    .meet-panel {
      background: #202124;
      border-left: 1px solid rgba(255, 255, 255, .08);
      display: flex;
      flex-direction: column;
      min-height: 0;
    }

    .meet-panel-header {
      padding: 20px 18px 14px;
      border-bottom: 1px solid rgba(255, 255, 255, .08);
    }

    .meet-panel-header h2 {
      margin: 0;
      font-size: 15px;
      font-weight: 600;
      color: #fff;
    }

    .meet-participants {
      flex: 1;
      overflow: auto;
      padding: 10px 12px 16px;
      display: grid;
      gap: 6px;
      align-content: start;
    }

    .participant-row {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 10px 12px;
      border-radius: 12px;
    }

    .participant-row:hover { background: rgba(255, 255, 255, .04); }

    .participant-avatar {
      width: 36px;
      height: 36px;
      border-radius: 50%;
      display: grid;
      place-items: center;
      font-size: 12px;
      font-weight: 600;
      color: #fff;
      background: #5f6368;
      flex-shrink: 0;
    }

    .participant-row.here .participant-avatar { background: #669df6; }

    .participant-name {
      flex: 1;
      min-width: 0;
      font-size: 14px;
      color: #e8eaed;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .participant-status {
      font-size: 12px;
      color: #9aa0a6;
      flex-shrink: 0;
    }

    .participant-row.here .participant-status { color: #81c995; }

    .meet-panel-footer {
      padding: 14px 16px 18px;
      border-top: 1px solid rgba(255, 255, 255, .08);
    }

    .meet-empty {
      padding: 32px 16px;
      text-align: center;
      color: #9aa0a6;
      font-size: 13px;
      line-height: 1.5;
    }

    @media (max-width: 960px) {
      :host { margin: -18px; }
      .meet-stage.with-panel { grid-template-columns: 1fr; }
      .meet-panel {
        position: absolute;
        top: 0;
        right: 0;
        bottom: 0;
        width: min(300px, 100%);
        z-index: 4;
        box-shadow: -16px 0 48px rgba(0, 0, 0, .4);
      }
      .meet-toolbar { padding: 10px 14px; }
      .meet-brand-copy h1 { font-size: 14px; }
      .meet-chip.feature { display: none; }
    }
  `],
  template: `
    <div class="meet-root">
      <header class="meet-toolbar">
        <div class="meet-brand">
          <div class="meet-logo" aria-hidden="true">▶</div>
          <div class="meet-brand-copy">
            <h1>{{ session()?.title || context()?.sessionTitle || 'Live session' }}</h1>
            <p>{{ session()?.formationTitle || 'Training session' }}</p>
          </div>
        </div>

        <div class="meet-toolbar-actions">
          <span class="meet-chip live"><span class="live-dot"></span> Live</span>
          @if (isTrainer()) {
            <span class="meet-chip feature">Auto attendance</span>
            <button
              class="meet-btn meet-btn-ghost"
              [class.active]="rosterOpen()"
              type="button"
              (click)="toggleRoster()">
              People
            </button>
          }
          <a class="meet-btn meet-btn-danger" [routerLink]="backRoute()">Leave</a>
        </div>
      </header>

      <div class="meet-stage" [class.with-panel]="isTrainer() && rosterOpen()">
        <div class="meet-video-wrap">
          @if (error()) { <div class="meet-toast error">{{ error() }}</div> }
          @if (message()) { <div class="meet-toast success">{{ message() }}</div> }

          @if (loading()) {
            <div class="meet-loading">
              <div class="meet-spinner"></div>
              <h2>Joining session</h2>
              <p>Check your camera and microphone, then enter when ready.</p>
            </div>
          }

          <div #jitsiContainer class="meet-video"></div>

          @if (!isTrainer() && !loading()) {
            <div class="learner-note">Stay in the call — attendance is saved automatically</div>
          }
        </div>

        @if (isTrainer() && rosterOpen()) {
          <aside class="meet-panel">
            <div class="meet-panel-header">
              <h2>Participants</h2>
            </div>

            <div class="meet-participants">
              @for (entry of report()?.learners || []; track entry.enrollmentId) {
                <div class="participant-row" [class.here]="entry.connected">
                  <div class="participant-avatar">{{ initials(entry.learnerFullName) }}</div>
                  <div class="participant-name">{{ entry.learnerFullName }}</div>
                  <div class="participant-status">{{ participantLabel(entry) }}</div>
                </div>
              } @empty {
                <div class="meet-empty">Waiting for learners to join.</div>
              }
            </div>

            @if (session()?.status === 'IN_PROGRESS') {
              <div class="meet-panel-footer">
                <button class="meet-btn meet-btn-primary" type="button" (click)="completeSmart()">
                  End session
                </button>
              </div>
            }
          </aside>
        }
      </div>
    </div>
  `
})
export class SessionClassroomComponent implements OnInit, OnDestroy {
  @ViewChild('jitsiContainer', { static: false }) jitsiContainer?: ElementRef<HTMLElement>;

  sessionId = 0;
  session = signal<TrainingSession | null>(null);
  context = signal<ClassroomContext | null>(null);
  report = signal<ClassroomAttendanceReport | null>(null);
  rosterOpen = signal(false);
  loading = signal(true);
  error = signal('');
  message = signal('');

  private jitsiApi?: { dispose: () => void; addListener: (event: string, listener: () => void) => void };
  private heartbeatTimer?: number;
  private rosterTimer?: number;
  private leaving = false;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly auth: AuthService,
    private readonly classroom: ClassroomService,
    private readonly sessions: SessionService,
    private readonly confirmDialog: ConfirmDialogService
  ) {}

  ngOnInit(): void {
    this.sessionId = Number(this.route.snapshot.paramMap.get('sessionId'));
    this.rosterOpen.set(window.innerWidth > 960 && this.isTrainer());

    this.sessions.get(this.sessionId).subscribe({
      next: (session) => this.session.set(session),
      error: () => this.error.set('Could not load session details.')
    });

    this.classroom.join(this.sessionId).subscribe({
      next: (context) => {
        this.context.set(context);
        window.setTimeout(() => this.initJitsi(context), 50);
        this.startHeartbeat(context.heartbeatIntervalSec);
        if (this.isTrainer()) {
          this.refreshRoster();
          this.rosterTimer = window.setInterval(() => this.refreshRoster(), 10000);
        }
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message || 'Could not join the session.');
      }
    });

    window.addEventListener('beforeunload', this.handleUnload);
  }

  ngOnDestroy(): void {
    window.removeEventListener('beforeunload', this.handleUnload);
    window.clearInterval(this.heartbeatTimer);
    window.clearInterval(this.rosterTimer);
    this.jitsiApi?.dispose();
    if (!this.leaving) {
      this.leaving = true;
      this.classroom.leave(this.sessionId).subscribe();
    }
  }

  isTrainer(): boolean {
    return this.auth.currentUser()?.role === 'TRAINER';
  }

  backRoute(): string {
    return this.isTrainer() ? '/trainer/my-sessions' : '/learner/my-enrollments';
  }

  toggleRoster(): void {
    this.rosterOpen.update((open) => !open);
  }

  participantLabel(entry: { connected: boolean }): string {
    return entry.connected ? 'Here' : 'Not joined';
  }

  initials(name: string): string {
    return name.split(/\s+/).filter(Boolean).slice(0, 2).map((p) => p[0]?.toUpperCase() || '').join('');
  }

  async completeSmart(): Promise<void> {
    const ok = await this.confirmDialog.confirm({
      title: 'End session?',
      message: 'End this session? Attendance will be saved automatically for learners who stayed in the call.',
      confirmLabel: 'End session',
      tone: 'primary'
    });
    if (!ok) return;
    this.classroom.completeSmart(this.sessionId).subscribe({
      next: (updated) => {
        this.message.set('Session ended. Attendance has been saved.');
        this.error.set('');
        this.session.set(updated);
      },
      error: (err) => this.error.set(err?.error?.message || 'Could not end the session.')
    });
  }

  private refreshRoster(): void {
    this.classroom.attendance(this.sessionId).subscribe({
      next: (report) => this.report.set(report),
      error: () => undefined
    });
  }

  private startHeartbeat(intervalSec: number): void {
    this.heartbeatTimer = window.setInterval(() => {
      this.classroom.heartbeat(this.sessionId).subscribe({ error: () => undefined });
    }, intervalSec * 1000);
  }

  private initJitsi(context: ClassroomContext): void {
    const parentNode = this.jitsiContainer?.nativeElement;
    if (!parentNode) return;

    this.loadJitsiScript(context.jitsiDomain).then(() => {
      const JitsiMeetExternalAPI = window.JitsiMeetExternalAPI;
      if (!JitsiMeetExternalAPI) {
        this.loading.set(false);
        this.error.set('Could not load video.');
        return;
      }

      this.jitsiApi = new JitsiMeetExternalAPI(context.jitsiDomain, {
        roomName: context.roomName,
        parentNode,
        width: '100%',
        height: '100%',
        userInfo: { displayName: context.displayName },
        configOverwrite: {
          prejoinPageEnabled: true,
          startWithAudioMuted: true,
          startWithVideoMuted: false,
          disableDeepLinking: true,
          enableWelcomePage: false,
          subject: context.sessionTitle,
          defaultLanguage: 'en'
        },
        interfaceConfigOverwrite: {
          SHOW_JITSI_WATERMARK: false,
          SHOW_WATERMARK_FOR_GUESTS: false,
          SHOW_BRAND_WATERMARK: false,
          DEFAULT_BACKGROUND: '#131314',
          TOOLBAR_ALWAYS_VISIBLE: true,
          MOBILE_APP_PROMO: false,
          VERTICAL_FILMSTRIP: true,
          DISABLE_JOIN_LEAVE_NOTIFICATIONS: true
        }
      });

      this.jitsiApi.addListener('videoConferenceJoined', () => this.loading.set(false));
      this.jitsiApi.addListener('readyToClose', () => {
        if (!this.leaving) {
          this.leaving = true;
          this.classroom.leave(this.sessionId).subscribe();
        }
      });

      window.setTimeout(() => this.loading.set(false), 12000);
    }).catch(() => {
      this.loading.set(false);
      this.error.set('Could not load video.');
    });
  }

  private loadJitsiScript(domain: string): Promise<void> {
    const src = `https://${domain}/external_api.js`;
    if (document.querySelector(`script[src="${src}"]`) && window.JitsiMeetExternalAPI) {
      return Promise.resolve();
    }
    return new Promise((resolve, reject) => {
      const script = document.createElement('script');
      script.src = src;
      script.async = true;
      script.onload = () => resolve();
      script.onerror = () => reject(new Error('Jitsi script failed'));
      document.body.appendChild(script);
    });
  }

  private handleUnload = (): void => {
    this.classroom.leave(this.sessionId).subscribe({ error: () => undefined });
  };
}
