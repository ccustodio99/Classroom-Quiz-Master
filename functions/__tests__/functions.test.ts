import * as admin from 'firebase-admin';
import firebaseFunctionsTest = require('firebase-functions-test');
import {
  exportReport,
  scoreAttempt,
  setBucketFactoryForTests,
  setFirestoreForTests,
  resetTestOverrides,
} from '../src/index';

describe('Cloud Functions', () => {
  const fft = firebaseFunctionsTest({ projectId: 'quizmaster-test' });

  afterEach(() => {
    resetTestOverrides();
    jest.restoreAllMocks();
  });

  afterAll(() => {
    fft.cleanup();
  });

  test('scoreAttempt computes points and updates submission', async () => {
    const attemptData = {
      uid: 'student1',
      questionId: 'q1',
      selected: ['A'],
      timeMs: 1_200,
    };
    const attemptRef = { id: 'attempt-1' } as admin.firestore.DocumentReference;
    const assignmentRef = { collection: jest.fn(() => ({ doc: () => submissionRef })) } as unknown as admin.firestore.DocumentReference;
    const quizRef = {} as admin.firestore.DocumentReference;
    const submissionRef = { id: 'student1' } as admin.firestore.DocumentReference;

    const assignmentData = {
      quizId: 'quiz1',
      attemptsAllowed: 3,
      scoringMode: 'best',
    };
    const quizData = {
      defaultTimePerQ: 30,
      questions: [
        { id: 'q1', answerKey: ['A'], timeLimitSeconds: 30 },
      ],
    };

    const transaction = {
      get: jest.fn(async (ref: unknown) => {
        if (ref === attemptRef) {
          return { exists: true, get: (field: string) => (attemptData as any)[field] };
        }
        if (ref === assignmentRef) {
          return { exists: true, data: () => assignmentData, get: (field: string) => (assignmentData as any)[field] };
        }
        if (ref === quizRef) {
          return { exists: true, data: () => quizData, get: (field: string) => (quizData as any)[field] };
        }
        if (ref === submissionRef) {
          return { exists: false, get: () => undefined };
        }
        throw new Error('Unexpected ref');
      }),
      update: jest.fn(),
      set: jest.fn(),
    };

    const firestoreStub = {
      collection: jest.fn((name: string) => {
        if (name === 'assignments') {
          return { doc: () => assignmentRef };
        }
        if (name === 'quizzes') {
          return { doc: () => quizRef };
        }
        throw new Error(`Unexpected collection ${name}`);
      }),
      runTransaction: jest.fn(async (fn: (txn: typeof transaction) => Promise<void>) => {
        await fn(transaction);
      }),
    } as unknown as admin.firestore.Firestore;

    setFirestoreForTests(firestoreStub);
    const snap = { data: () => attemptData, ref: attemptRef };
    const context = { params: { assignmentId: 'a1', attemptId: 'attempt-1' } } as any;

    const wrapped = fft.wrap(scoreAttempt as any);
    await wrapped(snap, context);

    expect(firestoreStub.runTransaction).toHaveBeenCalled();
    expect(transaction.update).toHaveBeenCalledWith(
      attemptRef,
      expect.objectContaining({ points: expect.any(Number), correct: true })
    );
    expect(transaction.set).toHaveBeenCalledWith(
      submissionRef,
      expect.objectContaining({ attempts: 1, bestScore: expect.any(Number) }),
      expect.objectContaining({ merge: true })
    );
  });

  test('exportReport validates ownership and returns signed URLs', async () => {
    const sessionData = { teacherId: 'teacher1', quizId: 'quiz1', classroomId: 'class1', name: 'Session A' };
    const quizData = {
      title: 'Quiz Title',
      category: 'pre',
      defaultTimePerQ: 30,
      topicId: 'topic1',
      questions: [{ id: 'q1', stem: 'Question text', answerKey: ['A'], choices: ['A', 'B'] }],
    };
    const classroomData = {
      name: 'Class 1',
      subject: 'Math',
      grade: '8',
      students: ['student1'],
      teacherId: 'teacher1',
    };
    const attempts = [
      {
        id: 'attempt-1',
        data: () => ({
          uid: 'student1',
          questionId: 'q1',
          selected: ['A'],
          points: 900,
          timeMs: 1_200,
          correct: true,
          createdAt: admin.firestore.Timestamp.fromMillis(1_000),
        }),
        get: (field: string) =>
          ({
            uid: 'student1',
            questionId: 'q1',
            selected: ['A'],
            points: 900,
            timeMs: 1_200,
            correct: true,
            createdAt: admin.firestore.Timestamp.fromMillis(1_000),
          } as any)[field],
      },
    ];

    const attemptsCollection = {
      get: jest.fn(async () => ({ size: attempts.length, docs: attempts })),
    };

    const sessionRef = {
      get: jest.fn(async () => ({ exists: true, get: (field: string) => (sessionData as any)[field] })),
      collection: jest.fn((name: string) => {
        if (name === 'attempts') {
          return attemptsCollection;
        }
        throw new Error(`Unexpected subcollection ${name}`);
      }),
    } as unknown as admin.firestore.DocumentReference;

    const quizRef = {
      get: jest.fn(async () => ({ exists: true, get: (field: string) => (quizData as any)[field] })),
    } as unknown as admin.firestore.DocumentReference;

    const classroomRef = {
      get: jest.fn(async () => ({ exists: true, get: (field: string) => (classroomData as any)[field] })),
    } as unknown as admin.firestore.DocumentReference;

    const teacherRef = {
      get: jest.fn(async () => ({ exists: true, get: () => 'Teacher Name' })),
    } as unknown as admin.firestore.DocumentReference;

    const studentsCollection = {
      where: jest.fn(() => ({
        get: jest.fn(async () => ({ docs: [] as admin.firestore.QueryDocumentSnapshot[] })),
      })),
    };

    const topicsCollection = {
      where: jest.fn(() => ({
        get: jest.fn(async () => ({ docs: [] as admin.firestore.QueryDocumentSnapshot[] })),
      })),
    };

    const firestoreStub = {
      collection: jest.fn((name: string) => {
        if (name === 'sessions') {
          return { doc: () => sessionRef };
        }
        if (name === 'quizzes') {
          return { doc: () => quizRef };
        }
        if (name === 'classrooms') {
          return { doc: () => classroomRef };
        }
        if (name === 'students') {
          return studentsCollection;
        }
        if (name === 'topics') {
          return topicsCollection;
        }
        if (name === 'teachers') {
          return { doc: () => teacherRef };
        }
        throw new Error(`Unexpected collection ${name}`);
      }),
      runTransaction: jest.fn(),
    } as unknown as admin.firestore.Firestore;

    const savedFiles: Record<string, { save: jest.Mock; getSignedUrl: jest.Mock; saved?: unknown }> = {};
    const bucket = {
      file: (path: string) => {
        if (!savedFiles[path]) {
          const record: any = {};
          record.save = jest.fn(async (content: unknown) => {
            record.saved = content;
          });
          record.getSignedUrl = jest.fn(async () => [`https://example.com/${path}`]);
          savedFiles[path] = record;
        }
        return savedFiles[path];
      },
    } as any;

    setFirestoreForTests(firestoreStub);
    setBucketFactoryForTests(() => bucket);

    const callable = fft.wrap(exportReport as any);
    const result = await callable(
      { sessionId: 's1' },
      { auth: { uid: 'teacher1', token: { firebase: { sign_in_provider: 'password' } } } }
    );

    expect(result.csvUrl).toContain('https://example.com/reports/s1/assessment-report');
    expect(result.pdfUrl).toContain('https://example.com/reports/s1/assessment-report');
    expect(result.accuracy).toBe(100);
    expect(result.exportVersion).toBe('v2');
    expect(Object.keys(savedFiles).length).toBe(2);
    const csvRecord = Object.entries(savedFiles).find(([key]) => key.endsWith('.csv'))?.[1];
    expect((csvRecord as any).saved as string).toContain('session_id');
    expect((csvRecord as any).saved as string).toContain('student_id');
  });
});
