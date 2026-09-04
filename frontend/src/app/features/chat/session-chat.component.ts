import { Component, OnDestroy, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ChatMessage, TypingEvent } from '../../core/models/chat.model';
import { TrainingSession } from '../../core/models/session.model';
import { AuthService } from '../../core/auth/auth.service';
import { ChatService } from '../../core/services/chat.service';
import { SessionService } from '../../core/services/session.service';
import { StompService } from '../../core/services/stomp.service';
import { ChatInputComponent } from './chat-input.component';
import { ChatMessageBubbleComponent } from './chat-message-bubble.component';

@Component({
  selector: 'app-session-chat',
  standalone: true,
  imports: [RouterLink, ChatInputComponent, ChatMessageBubbleComponent],
  template: `
    <section class="chat-shell">
      <header class="chat-header">
        <div>
          <span class="eyebrow">Live support</span>
          <h1>{{ session()?.title || 'Session chat' }}</h1>
          <p>{{ session()?.formationTitle }}</p>
        </div>
        <a class="btn btn-light" [routerLink]="backRoute()">Back</a>
      </header>

      <main class="chat-messages">
        @if (loading()) {
          <p class="muted">Loading messages...</p>
        } @else if (!messages().length) {
          <p class="muted empty-chat">No messages yet. Start the conversation.</p>
        }
        @for (message of messages(); track message.id) {
          <app-chat-message-bubble [message]="message" />
        }
      </main>

      @if (typingText()) {
        <div class="typing-indicator">{{ typingText() }}</div>
      }

      <app-chat-input (send)="send($event)" (typing)="sendTyping(true)" />
    </section>
  `
})
export class SessionChatComponent implements OnInit, OnDestroy {
  sessionId = 0;
  messages = signal<ChatMessage[]>([]);
  session = signal<TrainingSession | null>(null);
  loading = signal(true);
  typingText = signal('');
  private typingTimer?: number;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly auth: AuthService,
    private readonly chat: ChatService,
    private readonly sessions: SessionService,
    private readonly stomp: StompService
  ) {}

  ngOnInit(): void {
    this.sessionId = Number(this.route.snapshot.paramMap.get('sessionId'));
    this.sessions.get(this.sessionId).subscribe((session) => this.session.set(session));
    this.chat.history(this.sessionId).subscribe((messages) => {
      this.messages.set(messages);
      this.loading.set(false);
      this.chat.markAsRead(this.sessionId).subscribe();
    });
    this.stomp.connect();
    this.stomp.subscribe(`/topic/sessions/${this.sessionId}/chat`, (payload) => {
      const message = payload as ChatMessage;
      const currentUserId = this.auth.currentUser()?.id;
      this.messages.update((messages) => [...messages, { ...message, mine: message.senderId === currentUserId }]);
      if (message.senderId !== currentUserId) {
        this.playIncomingSound();
        this.chat.markAsRead(this.sessionId).subscribe();
      }
    });
    this.stomp.subscribe(`/topic/sessions/${this.sessionId}/typing`, (payload) => {
      const event = payload as TypingEvent;
      if (event.userId === this.auth.currentUser()?.id) return;
      this.typingText.set(event.typing ? `${event.fullName} is typing...` : '');
      window.clearTimeout(this.typingTimer);
      this.typingTimer = window.setTimeout(() => this.typingText.set(''), 1600);
    });
  }

  send(content: string): void {
    this.stomp.send(`/app/sessions/${this.sessionId}/chat.send`, { content });
    this.sendTyping(false);
  }

  sendTyping(typing: boolean): void {
    const user = this.auth.currentUser();
    if (!user) return;
    this.stomp.send(`/app/sessions/${this.sessionId}/chat.typing`, {
      sessionId: this.sessionId,
      userId: user.id,
      fullName: `${user.firstName} ${user.lastName}`,
      typing
    });
  }

  backRoute(): string {
    return this.auth.currentUser()?.role === 'TRAINER' ? '/trainer/my-sessions' : '/learner/my-enrollments';
  }

  ngOnDestroy(): void {
    window.clearTimeout(this.typingTimer);
    this.stomp.unsubscribe(`/topic/sessions/${this.sessionId}/chat`);
    this.stomp.unsubscribe(`/topic/sessions/${this.sessionId}/typing`);
  }

  private playIncomingSound(): void {
    const AudioContextClass = window.AudioContext || (window as unknown as { webkitAudioContext?: typeof AudioContext }).webkitAudioContext;
    if (!AudioContextClass) return;

    const context = new AudioContextClass();
    const now = context.currentTime;
    this.playTone(context, now, 740, 0.08);
    this.playTone(context, now + 0.09, 980, 0.09);
    window.setTimeout(() => context.close(), 350);
  }

  private playTone(context: AudioContext, startTime: number, frequency: number, duration: number): void {
    const oscillator = context.createOscillator();
    const gain = context.createGain();

    oscillator.type = 'sine';
    oscillator.frequency.setValueAtTime(frequency, startTime);
    gain.gain.setValueAtTime(0.0001, startTime);
    gain.gain.exponentialRampToValueAtTime(0.08, startTime + 0.01);
    gain.gain.exponentialRampToValueAtTime(0.0001, startTime + duration);

    oscillator.connect(gain);
    gain.connect(context.destination);
    oscillator.start(startTime);
    oscillator.stop(startTime + duration + 0.02);
  }
}
